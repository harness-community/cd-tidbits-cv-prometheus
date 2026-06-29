package com.harness.ecommerce.service;

import com.harness.ecommerce.exception.InsufficientStockException;
import com.harness.ecommerce.exception.ResourceNotFoundException;
import com.harness.ecommerce.model.*;
import com.harness.ecommerce.model.Order;
import com.harness.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("order")
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartService cartService;
    @Mock private ProductService productService;
    @Mock private UserService userService;
    @InjectMocks private OrderService orderService;

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
                .email("u" + id + "@test.com").password("pass").active(true).build();
    }

    Product stubProduct(Long id, BigDecimal price, int stock) {
        return Product.builder().id(id).name("Product " + id)
                .price(price).stock(stock).category("Test").active(true).build();
    }

    Cart cartWithItems(User user, List<CartItem> items) {
        Cart cart = Cart.builder().id(1L).user(user).build();
        items.forEach(i -> { i.setCart(cart); cart.getItems().add(i); });
        return cart;
    }

    // ── Checkout ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Checkout - success with single item")
    void checkout_singleItem_createsOrder() throws InterruptedException {
        User user = stubUser(1L);
        Product product = stubProduct(1L, new BigDecimal("49.99"), 100);
        CartItem item = CartItem.builder().product(product).quantity(2).build();
        Cart cart = cartWithItems(user, List.of(item));
        Order savedOrder = Order.builder().id(1L).user(user)
                .total(new BigDecimal("99.98")).items(new ArrayList<>()).build();

        when(cartService.getOrCreateCart(1L)).thenReturn(cart);
        when(userService.getById(1L)).thenReturn(user);
        when(productService.updateStock(anyLong(), anyInt())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(cartService.clearCart(anyLong())).thenReturn(cart);

        Thread.sleep(30);
        Order result = orderService.checkout(1L);
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Checkout - empty cart throws exception")
    void checkout_emptyCart_throwsException() {
        User user = stubUser(1L);
        Cart emptyCart = Cart.builder().id(1L).user(user).build();
        when(cartService.getOrCreateCart(1L)).thenReturn(emptyCart);
        assertThatThrownBy(() -> orderService.checkout(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    @DisplayName("Checkout - insufficient stock throws exception")
    void checkout_insufficientStock_throwsException() {
        User user = stubUser(1L);
        Product product = stubProduct(1L, new BigDecimal("10.00"), 1);
        CartItem item = CartItem.builder().product(product).quantity(5).build();
        Cart cart = cartWithItems(user, List.of(item));
        when(cartService.getOrCreateCart(1L)).thenReturn(cart);
        assertThatThrownBy(() -> orderService.checkout(1L))
                .isInstanceOf(InsufficientStockException.class);
    }

    // ── Get By ID ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "getById [{index}] id={0}")
    @ValueSource(longs = {1, 2, 5, 10, 100})
    @DisplayName("Get order by ID - found")
    void getById_existing_returnsOrder(Long id) throws InterruptedException {
        Order order = Order.builder().id(id).total(BigDecimal.TEN)
                .items(new ArrayList<>()).status(Order.Status.PENDING).build();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        Thread.sleep(20);
        Order result = orderService.getById(id);
        assertThat(result.getId()).isEqualTo(id);
    }

    @ParameterizedTest(name = "getById_notFound [{index}] id={0}")
    @ValueSource(longs = {999, 10000})
    @DisplayName("Get order by ID - not found throws exception")
    void getById_missing_throwsException(Long id) {
        when(orderRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Status Updates ────────────────────────────────────────────────────────

    @ParameterizedTest(name = "updateStatus [{index}] to={0}")
    @EnumSource(value = Order.Status.class, names = {"CONFIRMED", "SHIPPED", "DELIVERED"})
    @DisplayName("Update order status - valid transitions")
    void updateStatus_validTransition_updatesStatus(Order.Status newStatus) throws InterruptedException {
        Order order = Order.builder().id(1L).total(BigDecimal.TEN)
                .items(new ArrayList<>()).status(Order.Status.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Thread.sleep(20);
        Order result = orderService.updateStatus(1L, newStatus);
        assertThat(result.getStatus()).isEqualTo(newStatus);
    }

    @Test
    @DisplayName("Update status - cancelled order throws exception")
    void updateStatus_cancelledOrder_throwsException() {
        Order order = Order.builder().id(1L).total(BigDecimal.TEN)
                .items(new ArrayList<>()).status(Order.Status.CANCELLED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> orderService.updateStatus(1L, Order.Status.CONFIRMED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "cancel_pending [{index}] status={0}")
    @EnumSource(value = Order.Status.class, names = {"PENDING", "CONFIRMED"})
    @DisplayName("Cancel order - cancellable statuses succeed")
    void cancelOrder_cancellableStatus_cancels(Order.Status status) throws InterruptedException {
        Product product = stubProduct(1L, new BigDecimal("10.00"), 5);
        OrderItem orderItem = OrderItem.builder().product(product).quantity(2)
                .priceAtPurchase(new BigDecimal("10.00")).build();
        Order order = Order.builder().id(1L).total(BigDecimal.TEN)
                .items(new ArrayList<>(List.of(orderItem)))
                .status(status).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productService.updateStock(anyLong(), anyInt())).thenReturn(product);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Thread.sleep(25);
        Order result = orderService.cancelOrder(1L);
        assertThat(result.getStatus()).isEqualTo(Order.Status.CANCELLED);
    }

    @ParameterizedTest(name = "cancel_nonCancellable [{index}] status={0}")
    @EnumSource(value = Order.Status.class, names = {"SHIPPED", "DELIVERED"})
    @DisplayName("Cancel order - non-cancellable statuses throw exception")
    void cancelOrder_nonCancellableStatus_throwsException(Order.Status status) {
        Order order = Order.builder().id(1L).total(BigDecimal.TEN)
                .items(new ArrayList<>()).status(status).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "totalSpend [{index}] userId={0}")
    @ValueSource(longs = {1, 2, 3, 4, 5})
    @DisplayName("Get total spend per user")
    void getTotalSpend_returnsAmount(Long userId) throws InterruptedException {
        when(orderRepository.sumTotalByUserId(userId)).thenReturn(new BigDecimal("250.00"));
        Thread.sleep(20);
        BigDecimal result = orderService.getTotalSpend(userId);
        assertThat(result).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Get total spend - null result returns zero")
    void getTotalSpend_nullResult_returnsZero() {
        when(orderRepository.sumTotalByUserId(anyLong())).thenReturn(null);
        assertThat(orderService.getTotalSpend(99L)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Get All Orders ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllOrders - returns all orders regardless of status")
    void getAllOrders_returnsAllOrders() throws InterruptedException {
        User user = User.builder().id(1L).name("Demo User").email("demo@harness.io")
                .password("1harness").active(true).build();
        Order o1 = Order.builder().id(1L).user(user).total(new BigDecimal("50.00"))
                .status(Order.Status.PENDING).build();
        Order o2 = Order.builder().id(2L).user(user).total(new BigDecimal("120.00"))
                .status(Order.Status.DELIVERED).build();
        Order o3 = Order.builder().id(3L).user(user).total(new BigDecimal("75.00"))
                .status(Order.Status.CANCELLED).build();
        when(orderRepository.findAll()).thenReturn(List.of(o1, o2, o3));
        Thread.sleep(20);
        List<Order> result = orderService.getAllOrders();
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Order::getStatus)
                .containsExactlyInAnyOrder(
                        Order.Status.PENDING,
                        Order.Status.DELIVERED,
                        Order.Status.CANCELLED);
    }

    @Test
    @DisplayName("getAllOrders - returns empty list when no orders exist")
    void getAllOrders_noOrders_returnsEmptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());
        List<Order> result = orderService.getAllOrders();
        assertThat(result).isEmpty();
    }
}
