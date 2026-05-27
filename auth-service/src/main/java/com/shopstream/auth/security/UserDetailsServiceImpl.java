package com.shopstream.auth.security;

import com.shopstream.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /*
     * Spring Security calls this when authenticating.
     * We load the user from DB by email (our username).
     * Return a UserDetails object with email, hashed password,
     * and authorities (roles).
     * Spring Security then compares the provided password
     * with the stored BCrypt hash automatically.
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        // ROLE_ prefix is Spring Security convention
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email));
    }
}