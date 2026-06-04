function validateForm() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const email = document.getElementById('email').value.trim();
    const btn = document.getElementById('retrieveBtn');

    if (username !== "" && password !== "" && email !== "") {
        btn.removeAttribute('disabled');
    } else {
        btn.setAttribute('disabled', 'true');
    }
}

async function handleRetrieveData() {
    const btn = document.getElementById('retrieveBtn');
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    const email = document.getElementById('email').value.trim();

    btn.classList.add('loading');
    btn.textContent = 'Fetching Environmental Data...';

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
            alert('Failed to contact server. Please check credentials or backend status.');
        }
    } catch (error) {
        console.error("Error fetching data:", error);
        alert('An error occurred while connecting to the predictive pipeline.');
    } finally {
        btn.classList.remove('loading');
        btn.textContent = 'Retrieve Data';
    }
}