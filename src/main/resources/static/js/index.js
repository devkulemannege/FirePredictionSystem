function validateForm() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const email = document.getElementById('email').value.trim();
    const btn = document.getElementById('retrieveBtn');
    const emailInput = document.getElementById('email');
    const emailError = document.getElementById('emailError');

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
    const isEmailValid = emailRegex.test(email);

    if (email !== '' && !isEmailValid) {
        emailInput.style.borderColor = '#ff4d4d';
        emailInput.style.outline = 'none';
        emailError.style.display = 'block';
    } else if (email !== '' && isEmailValid) {
        emailInput.style.borderColor = '#4caf50';
        emailInput.style.outline = 'none';
        emailError.style.display = 'none';
    } else {
        emailInput.style.borderColor = '';
        emailError.style.display = 'none';
    }

    if (username !== '' && password !== '' && isEmailValid) {
        btn.removeAttribute('disabled');
    } else {
        btn.setAttribute('disabled', 'true');
    }
}

// Modal Presentation Controller
function presentStatusModal(title, message, visualVariant = 'error') {
    const overlay = document.getElementById('statusModal');
    const box = document.getElementById('modalBox');
    const titleElem = document.getElementById('modalTitle');
    const msgElem = document.getElementById('modalMessage');
    const iconElem = document.getElementById('modalIcon');

    titleElem.textContent = title;
    msgElem.textContent = message;
    iconElem.textContent = (visualVariant === 'success') ? '✓' : '!';

    box.classList.remove('success-variant', 'error-variant');
    box.classList.add(`${visualVariant}-variant`);

    overlay.classList.add('active');
}

function dismissModal() {
    document.getElementById('statusModal').classList.remove('active');
}

async function handleRetrieveData() {
    const btn = document.getElementById('retrieveBtn');
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const email = document.getElementById('email').value.trim();

    btn.classList.add('loading');
    btn.textContent = 'Verifying Credentials...';

    try {
        const reply = await fetch('/request_api_data', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                username: username,
                password: password,
                email: email
            })
        });

        if (reply.ok) {
            window.location.href = "/task_form";
        } else {
            presentStatusModal(
                'Authentication Failed',
                'Failed to contact server. Please verify your Earthdata credentials and try again.',
                'error'
            );
        }
    } catch (error) {
        console.error("Error fetching data:", error);
        presentStatusModal(
            'Authentication Failed',
            'An unexpected error occurred while attempting to authenticate credentials.',
            'error'
        );
    } finally {
        btn.classList.remove('loading');
        btn.textContent = 'Retrieve Data';
    }
}