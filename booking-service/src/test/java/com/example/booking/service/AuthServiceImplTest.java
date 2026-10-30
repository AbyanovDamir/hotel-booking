package com.example.booking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.booking.dto.AuthRequest;
import com.example.booking.entity.Role;
import com.example.booking.entity.UserAccount;
import com.example.booking.exception.ConflictException;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication service")
class AuthServiceImplTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthServiceImpl service;

    @Test
    @DisplayName("регистрация шифрует пароль, назначает USER и выдаёт токен")
    void registersUser() {
        AuthRequest request = new AuthRequest("alice", "secret");
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issue(any(UserAccount.class))).thenReturn("jwt-token");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        var response = service.register(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("дублирующее имя пользователя отклоняется")
    void rejectsDuplicateUsername() {
        when(users.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new AuthRequest("alice", "secret")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("верные учётные данные возвращают токен")
    void authenticatesUser() {
        UserAccount user = UserAccount.builder().username("alice").password("encoded")
                .role(Role.USER).build();
        when(users.findByUsername("alice")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtService.issue(user)).thenReturn("jwt-token");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        var response = service.authenticate(new AuthRequest("alice", "secret"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("неверный пароль возвращает BadCredentialsException")
    void rejectsWrongPassword() {
        UserAccount user = UserAccount.builder().username("alice").password("encoded")
                .role(Role.USER).build();
        when(users.findByUsername("alice")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(new AuthRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).issue(any());
    }
}
