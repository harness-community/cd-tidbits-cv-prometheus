package com.harness.ecommerce.service;

import com.harness.ecommerce.exception.ResourceNotFoundException;
import com.harness.ecommerce.model.User;
import com.harness.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("user")
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    static User buildUser(Long id, String name, String email) {
        return User.builder().id(id).name(name).email(email)
                .password("SecurePass1!").active(true).build();
    }

    static Stream<User> userProvider() {
        return Stream.of(
            buildUser(1L, "Alice Smith",   "alice@example.com"),
            buildUser(2L, "Bob Johnson",   "bob@example.com"),
            buildUser(3L, "Carol White",   "carol@example.com"),
            buildUser(4L, "Dave Brown",    "dave@example.com"),
            buildUser(5L, "Eve Davis",     "eve@example.com"),
            buildUser(6L, "Frank Wilson",  "frank@example.com"),
            buildUser(7L, "Grace Lee",     "grace@example.com"),
            buildUser(8L, "Henry Martin",  "henry@example.com")
        );
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "createUser [{index}] {1}")
    @MethodSource("userProvider")
    @DisplayName("Create user - valid users")
    void createUser_validUser_returnsSaved(User user) throws InterruptedException {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        Thread.sleep(25);
        User result = userService.createUser(user);
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        verify(userRepository).save(user);
    }

    @ParameterizedTest(name = "createUser_duplicate [{index}] {0}")
    @ValueSource(strings = {"taken@test.com", "used@example.com", "existing@harness.io"})
    @DisplayName("Create user - duplicate email throws exception")
    void createUser_duplicateEmail_throwsException(String email) {
        User user = buildUser(null, "Test User", email);
        when(userRepository.existsByEmail(email)).thenReturn(true);
        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(email);
    }

    // ── Get ───────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "getById [{index}] id={0}")
    @ValueSource(longs = {1, 2, 3, 5, 10, 100})
    @DisplayName("Get user by ID - found")
    void getById_existingId_returnsUser(Long id) throws InterruptedException {
        User user = buildUser(id, "User " + id, "user" + id + "@test.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        Thread.sleep(20);
        User result = userService.getById(id);
        assertThat(result.getId()).isEqualTo(id);
    }

    @ParameterizedTest(name = "getById_notFound [{index}] id={0}")
    @ValueSource(longs = {999, 10000, 55555})
    @DisplayName("Get user by ID - not found throws exception")
    void getById_missingId_throwsException(Long id) {
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest(name = "getByEmail [{index}] {0}")
    @ValueSource(strings = {"alice@example.com", "bob@example.com", "carol@example.com"})
    @DisplayName("Get user by email - found")
    void getByEmail_existingEmail_returnsUser(String email) throws InterruptedException {
        User user = buildUser(1L, "Test", email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Thread.sleep(20);
        User result = userService.getByEmail(email);
        assertThat(result.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Get user by email - not found throws exception")
    void getByEmail_missing_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getByEmail("ghost@nowhere.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "updateUser [{index}] newName={0}")
    @CsvSource({
        "Alice Updated, alice@example.com",
        "Bob New Name,  bob@example.com",
        "Carol Changed, carol@example.com"
    })
    @DisplayName("Update user - valid updates")
    void updateUser_validData_returnsUpdated(String newName, String email) throws InterruptedException {
        User existing = buildUser(1L, "Old Name", email);
        User updated  = buildUser(1L, newName, email);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Thread.sleep(20);
        User result = userService.updateUser(1L, updated);
        assertThat(result.getName()).isEqualTo(newName);
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deactivate user - sets active=false")
    void deactivate_existingUser_setsInactive() {
        User user = buildUser(1L, "To Deactivate", "deactivate@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        userService.deactivate(1L);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Email exists - returns true when present")
    void emailExists_existing_returnsTrue() {
        when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);
        assertThat(userService.emailExists("exists@test.com")).isTrue();
    }

    @Test
    @DisplayName("Count active users - delegates to repository")
    void countActiveUsers_returnsCount() {
        when(userRepository.countByActiveTrue()).thenReturn(42L);
        assertThat(userService.countActiveUsers()).isEqualTo(42L);
    }
}
