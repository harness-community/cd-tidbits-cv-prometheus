package com.harness.ecommerce.service;

import com.harness.ecommerce.exception.InsufficientStockException;
import com.harness.ecommerce.model.*;
import com.harness.ecommerce.repository.CartRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("cart")
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private UserService userService;
    @Mock private ProductService productService;
    @InjectMocks private CartService cartService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    User stubUser(Long id) {
        return User.builder().id(id).name("User " + id)
                .email("user" + id + "@test.com").password("pass").active(true).build();
    }

    Product stubProduct(Long id, BigDecimal price, int stock) {
        return Product.builder().id(id).name("Product " + id)
                .price(price).stock(stock).category("Test").active(true).build();
    }

    Cart emptyCart(User user) {
        return Cart.builder().id(1L).user(user).build();
    }

    // ── Get or Create Cart ────────────────────────────────────────────────────

    @ParameterizedTest(name = "getOrCreate [{index}] userId={0}")
    @ValueSource(longs = {1, 2, 3, 4, 5})
    @DisplayName("Get or create cart - creates new cart if absent")
    void getOrCreateCart_noExisting_createsNew(Long userId) throws InterruptedException {
        User user = stubUser(userId);
        Cart newCart = emptyCart(user);
        when(userService.getById(userId)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenReturn(newCart);
        Thread.sleep(20);
        Cart result = cartService.getOrCreateCart(userId);
        assertThat(result).isNotNull();
    }

    @ParameterizedTest(name = "getOrCreate_existing [{index}] userId={0}")
    @ValueSource(longs = {1, 2, 3})
    @DisplayName("Get or create cart - returns existing cart")
    void getOrCreateCart_existing_returnsExisting(Long userId) throws InterruptedException {
        User user = stubUser(userId);
        Cart existingCart = emptyCart(user);
        when(userService.getById(userId)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(existingCart));
        Thread.sleep(15);
        Cart result = cartService.getOrCreateCart(userId);
        assertThat(result.getId()).isEqualTo(existingCart.getId());
    }

    // ── Add Item ──────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "addItem [{index}] qty={0}")
    @ValueSource(ints = {1, 2, 5, 10, 20})
    @DisplayName("Add item to cart - valid quantities")
    void addItem_validQuantity_addsToCart(int quantity) throws InterruptedException {
        User user = stubUser(1L);
        Cart cart = emptyCart(user);
        Product product = stubProduct(1L, new BigDecimal("19.99"), 100);
        when(userService.getById(1L)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productService.getById(1L)).thenReturn(product);
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Thread.sleep(25);
        Cart result = cartService.addItem(1L, 1L, quantity);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(quantity);
    }

    @ParameterizedTest(name = "addItem_zeroOrNeg [{index}] qty={0}")
    @ValueSource(ints = {0, -1, -5, -100})
    @DisplayName("Add item - zero or negative quantity throws exception")
    void addItem_invalidQuantity_throwsException(int quantity) {
        User user = stubUser(1L);
        when(userService.getById(1L)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(emptyCart(user)));
        Product product = stubProduct(1L, new BigDecimal("10.00"), 50);
        when(productService.getById(1L)).thenReturn(product);
        assertThatThrownBy(() -> cartService.addItem(1L, 1L, quantity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Add item - insufficient stock throws InsufficientStockException")
    void addItem_insufficientStock_throwsException() {
        User user = stubUser(1L);
        Cart cart = emptyCart(user);
        Product product = stubProduct(1L, new BigDecimal("10.00"), 3);
        when(userService.getById(1L)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productService.getById(1L)).thenReturn(product);
        assertThatThrownBy(() -> cartService.addItem(1L, 1L, 10))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Add item - inactive product throws exception")
    void addItem_inactiveProduct_throwsException() {
        User user = stubUser(1L);
        Cart cart = emptyCart(user);
        Product product = stubProduct(1L, new BigDecimal("10.00"), 50);
        product.setActive(false);
        when(userService.getById(1L)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productService.getById(1L)).thenReturn(product);
        assertThatThrownBy(() -> cartService.addItem(1L, 1L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    // ── Update Quantity ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "updateQty [{index}] newQty={0}")
    @CsvSource({"1", "3", "5", "10"})
    @DisplayName("Update item quantity - valid values")
    void updateItemQuantity_valid_updatesQuantity(int newQty) throws InterruptedException {
        User user = stubUser(1L);
        Product product = stubProduct(1L, new BigDecimal("10.00"), 50);
        CartItem item = CartItem.builder().id(1L).product(product).quantity(2).build();
        Cart cart = emptyCart(user);
        cart.getItems().add(item);
        item.setCart(cart);
        when(userService.getById(1L)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Thread.sleep(20);
        Cart result = cartService.updateItemQuantity(1L, 1L, newQty);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(newQty);
    }

    // ── Clear Cart ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Clear cart - removes all items")
    void clearCart_withItems_emptiesCart() throws InterruptedException {
        User user = stubUser(1L);
        Product product = stubProduct(1L, new BigDecimal("10.00"), 50);
        CartItem item = CartItem.builder().id(1L).product(product).quantity(2).build();
        Cart cart = emptyCart(user);
        cart.getItems().add(item);
        when(userService.getById(1L)).thenReturn(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Thread.sleep(20);
        Cart result = cartService.clearCart(1L);
        assertThat(result.getItems()).isEmpty();
    }
}
