package com.nexus.chat;

import com.nexus.AbstractIntegrationTest;
import com.nexus.chat.dto.ConversationSummaryResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the conversation-list N+1 fix. listConversations batches its
 * peer-username and unread-count lookups, so the number of SQL statements it issues must
 * stay flat regardless of how many conversations the user has. If the batching regresses
 * into a per-conversation query, the statement count scales with the conversation count;
 * asserting "fewer statements than conversations" fails the moment that happens.
 */
@Transactional
class ConversationListQueryCountTest extends AbstractIntegrationTest {

    @Autowired private ChatService chat;
    @Autowired private UserRepository users;

    @PersistenceContext private EntityManager em;

    @Test
    void listConversationsDoesNotScaleQueriesWithConversationCount() {
        int peers = 10;
        users.save(new User("qc_owner", "h"));
        for (int i = 0; i < peers; i++) {
            users.save(new User("qc_peer_" + i, "h"));
            chat.sendDirectMessage("qc_owner",
                    new SendDirectMessageRequest("qc_peer_" + i, "hi " + i));
        }

        Statistics stats = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        // Detach everything so the measured call genuinely hits the database.
        em.flush();
        em.clear();
        stats.clear();

        List<ConversationSummaryResponse> list = chat.listConversations("qc_owner");

        assertThat(list).hasSize(peers);
        long statements = stats.getPrepareStatementCount();
        assertThat(statements)
                .as("listConversations issued %d statements for %d conversations — "
                        + "batched lookups should keep this well below one-per-conversation",
                        statements, peers)
                .isLessThan(peers);
    }
}
