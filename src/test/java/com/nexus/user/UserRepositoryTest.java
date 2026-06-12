package com.nexus.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository users;

    @Test
    void savesAndFindsUserByUsername() {
        User saved = users.save(new User("roundtrip_user", "hashed-pw"));

        assertThat(saved.getId()).isNotNull();
        assertThat(users.existsByUsername("roundtrip_user")).isTrue();

        User found = users.findByUsername("roundtrip_user").orElseThrow();
        assertThat(found.getPasswordHash()).isEqualTo("hashed-pw");
    }
}
