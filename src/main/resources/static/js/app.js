// ── State ─────────────────────────────────────────────
let currentUser = null;   // { id, username, password }
let allProducts = [];

// ── Auth helpers ───────────────────────────────────────
function getAuthHeader() {
    const credentials = btoa(`${currentUser.username}:${currentUser.password}`);
    return { 'Authorization': `Basic ${credentials}`, 'Content-Type': 'application/json' };
}

// ── Tab switching ──────────────────────────────────────
function showTab(tab) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    event.target.classList.add('active');
    document.getElementById('login-form').style.display    = tab === 'login'    ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
    document.getElementById('auth-error').textContent = '';
}

// ── Register ───────────────────────────────────────────
async function register() {
    const username = document.getElementById('reg-username').value.trim();
    const email    = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    const errorEl  = document.getElementById('auth-error');
    errorEl.textContent = '';

    try {
        const res = await fetch('/api/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });
        const data = await res.json();

        if (!res.ok) {
            errorEl.textContent = data.error || 'Registration failed';
            return;
        }

        currentUser = { id: data.id, username, password };
        showApp();

    } catch (err) {
        errorEl.textContent = 'Network error. Is the server running?';
    }
}

// ── Login ──────────────────────────────────────────────
// Simple approach: verify credentials by calling a protected endpoint,
// then fetch the user record by username to get the ID.
async function login() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const errorEl  = document.getElementById('auth-error');
    errorEl.textContent = '';

    try {
        const credentials = btoa(`${username}:${password}`);

        // Any authenticated endpoint works as a credential check
        const checkRes = await fetch('/api/cart?userId=1', {
            headers: { 'Authorization': `Basic ${credentials}` }
        });

        if (checkRes.status === 401) {
            errorEl.textContent = 'Wrong username or password';
            return;
        }

        currentUser = { username, password, id: 1 }; // demo: replace with real lookup if needed
        showApp();

    } catch (err) {
        errorEl.textContent = 'Login failed. Please try again.';
    }
}

function logout() {
    currentUser = null;
    document.getElementById('auth-screen').style.display = 'flex';
    document.getElementById('app-screen').style.display  = 'none';
    document.getElementById('login-username').value = '';
    document.getElementById('login-password').value = '';
}

// ── Show main app ──────────────────────────────────────
function showApp() {
    document.getElementById('auth-screen').style.display = 'none';
    document.getElementById('app-screen').style.display  = 'block';
    document.getElementById('welcome-msg').textContent = `Hi, ${currentUser.username}`;
    loadProducts();
    updateCartCount();
}

// ── Page navigation ────────────────────────────────────
function showPage(page, btn) {
    document.querySelectorAll('.page').forEach(p => p.style.display = 'none');
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.getElementById(`page-${page}`).style.display = 'block';
    btn.classList.add('active');

    if (page === 'cart')   loadCart();
    if (page === 'orders') loadOrders();
}

// ── Products ───────────────────────────────────────────
async function loadProducts() {
    const res = await fetch('/api/products');
    allProducts = await res.json();
    renderProducts(allProducts);
}

async function searchProducts() {
    const name     = document.getElementById('search-input').value.trim();
    const category = document.getElementById('category-filter').value;

    const params = new URLSearchParams();
    if (name)     params.append('name', name);
    if (category) params.append('category', category);

    const res = await fetch(`/api/products?${params.toString()}`);
    const products = await res.json();
    renderProducts(products);
}

function renderProducts(products) {
    const grid = document.getElementById('product-grid');

    if (products.length === 0) {
        grid.innerHTML = `<div class="empty-state">No products found.</div>`;
        return;
    }

    grid.innerHTML = products.map(p => `
        <div class="product-card">
            <div class="product-img">
                ${p.imageUrl ? `<img src="${p.imageUrl}" alt="${escapeHtml(p.name)}">` : '📦'}
            </div>
            <div class="product-body">
                <div class="product-name">${escapeHtml(p.name)}</div>
                <div class="product-cat">${escapeHtml(p.category || '')}</div>
                <div class="product-price">₹${Number(p.price).toFixed(2)}</div>
                <div class="product-stock ${p.stock === 0 ? 'out' : ''}">
                    ${p.stock > 0 ? p.stock + ' in stock' : 'Out of stock'}
                </div>
                <button class="btn-add" ${p.stock === 0 ? 'disabled' : ''}
                        onclick="addToCart(${p.id})">
                    ${p.stock === 0 ? 'Unavailable' : 'Add to Cart'}
                </button>
            </div>
        </div>
    `).join('');
}

// ── Cart ───────────────────────────────────────────────
async function addToCart(productId) {
    try {
        const res = await fetch('/api/cart', {
            method: 'POST',
            headers: getAuthHeader(),
            body: JSON.stringify({ userId: currentUser.id, productId, quantity: 1 })
        });
        const data = await res.json();

        if (!res.ok) {
            alert(data.error || 'Failed to add to cart');
            return;
        }
        updateCartCount();
    } catch (err) {
        alert('Failed to add to cart');
    }
}

async function loadCart() {
    const res = await fetch(`/api/cart?userId=${currentUser.id}`, { headers: getAuthHeader() });
    const items = await res.json();
    renderCart(items);

    const totalRes = await fetch(`/api/cart/total?userId=${currentUser.id}`, { headers: getAuthHeader() });
    const totalData = await totalRes.json();
    document.getElementById('cart-total-amount').textContent = Number(totalData.total).toFixed(2);
}

function renderCart(items) {
    const list = document.getElementById('cart-list');

    if (items.length === 0) {
        list.innerHTML = `<div class="empty-state">Your cart is empty.</div>`;
        return;
    }

    list.innerHTML = items.map(item => `
        <div class="cart-item">
            <div class="cart-item-info">
                <div class="cart-item-name">${escapeHtml(item.product.name)}</div>
                <div class="cart-item-price">₹${Number(item.product.price).toFixed(2)} each</div>
            </div>
            <div class="qty-control">
                <button class="qty-btn" onclick="changeQty(${item.id}, ${item.quantity - 1})">-</button>
                <span>${item.quantity}</span>
                <button class="qty-btn" onclick="changeQty(${item.id}, ${item.quantity + 1})">+</button>
            </div>
            <div class="cart-item-subtotal">₹${(item.product.price * item.quantity).toFixed(2)}</div>
            <button class="btn-delete" onclick="removeCartItem(${item.id})">✕</button>
        </div>
    `).join('');
}

async function changeQty(cartItemId, newQty) {
    await fetch(`/api/cart/${cartItemId}`, {
        method: 'PUT',
        headers: getAuthHeader(),
        body: JSON.stringify({ userId: currentUser.id, quantity: newQty })
    });
    loadCart();
    updateCartCount();
}

async function removeCartItem(cartItemId) {
    await fetch(`/api/cart/${cartItemId}?userId=${currentUser.id}`, {
        method: 'DELETE',
        headers: getAuthHeader()
    });
    loadCart();
    updateCartCount();
}

async function updateCartCount() {
    const res = await fetch(`/api/cart?userId=${currentUser.id}`, { headers: getAuthHeader() });
    const items = await res.json();
    const count = items.reduce((sum, item) => sum + item.quantity, 0);
    document.getElementById('cart-count').textContent = count;
}

// ── Checkout ───────────────────────────────────────────
async function checkout() {
    const address = document.getElementById('shipping-address').value.trim();
    const errorEl = document.getElementById('checkout-error');
    errorEl.textContent = '';

    if (!address) {
        errorEl.textContent = 'Please enter a shipping address';
        return;
    }

    try {
        const res = await fetch('/api/orders/checkout', {
            method: 'POST',
            headers: getAuthHeader(),
            body: JSON.stringify({ userId: currentUser.id, shippingAddress: address })
        });
        const data = await res.json();

        if (!res.ok) {
            errorEl.textContent = data.error || 'Checkout failed';
            return;
        }

        document.getElementById('shipping-address').value = '';
        alert('Order placed successfully!');
        updateCartCount();
        loadCart();

    } catch (err) {
        errorEl.textContent = 'Checkout failed';
    }
}

// ── Orders ─────────────────────────────────────────────
async function loadOrders() {
    const res = await fetch(`/api/orders?userId=${currentUser.id}`, { headers: getAuthHeader() });
    const orders = await res.json();
    renderOrders(orders);
}

function renderOrders(orders) {
    const list = document.getElementById('orders-list');

    if (orders.length === 0) {
        list.innerHTML = `<div class="empty-state">No orders yet.</div>`;
        return;
    }

    list.innerHTML = orders.map(order => `
        <div class="order-card">
            <div class="order-header">
                <span class="order-id">Order #${order.id}</span>
                <span class="order-status status-${order.status}">${order.status}</span>
            </div>
            ${order.items.map(item => `
                <div class="order-item-row">
                    <span>${escapeHtml(item.product.name)} × ${item.quantity}</span>
                    <span>₹${(item.priceAtPurchase * item.quantity).toFixed(2)}</span>
                </div>
            `).join('')}
            <div class="order-item-row order-total">
                <span>Total</span>
                <span>₹${Number(order.totalAmount).toFixed(2)}</span>
            </div>
            <div class="order-actions">
                ${order.status === 'PENDING'
                    ? `<button class="btn-secondary" onclick="cancelOrder(${order.id})">Cancel Order</button>`
                    : ''}
            </div>
        </div>
    `).join('');
}

async function cancelOrder(orderId) {
    if (!confirm('Cancel this order?')) return;

    await fetch(`/api/orders/${orderId}/cancel?userId=${currentUser.id}`, {
        method: 'PATCH',
        headers: getAuthHeader()
    });
    loadOrders();
}

// ── Utils ──────────────────────────────────────────────
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
              .replace(/"/g,'&quot;').replace(/'/g,'&#039;');
}
