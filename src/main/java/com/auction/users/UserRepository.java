package com.auction.users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing {@link User} entities. */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);
}
