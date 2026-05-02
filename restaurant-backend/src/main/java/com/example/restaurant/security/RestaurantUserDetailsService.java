package com.example.restaurant.security;

import com.example.restaurant.entity.User;
import com.example.restaurant.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;

    public RestaurantUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        String role = "ROLE_" + user.getRoleName();
        return new org.springframework.security.core.userdetails.User(
                user.getPhone(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getEnabled()),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
