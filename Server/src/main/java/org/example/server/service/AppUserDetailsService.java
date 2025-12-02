package org.example.server.service;

import org.example.server.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Logic xử lý Phone: Nếu nhập 09xx -> check thêm +849xx
        String phoneFormatted = login;
        if (login != null && login.matches("^0\\d{9}$")) {
            phoneFormatted = "+84" + login.substring(1);
        }

        var user = userRepository.findByLoginIdentifier(login, phoneFormatted)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + login));

        var authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), authorities);
    }



}
