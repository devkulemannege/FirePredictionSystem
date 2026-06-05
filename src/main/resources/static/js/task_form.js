let structuralRedirectTargetUrl = null;
let tokenVerificationSuccessState = false;

// Toggle blur visualization
function toggleTokenObfuscation() {
    if (!tokenVerificationSuccessState) return;
    const display = document.getElementById('accessTokenDisplay');
    display.classList.toggle('is-blurred');
}

// Restrict date input to past or current dates only
function restrictFutureDates() {
    const dateInput = document.getElementById('targetDate');
    const today = new Date();

    const year = today.getFullYear();
    // Months are 0-indexed, so pad start with a leading zero if needed
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');

    // Format must perfectly match YYYY-MM-DD
    const maxDate = `${year}-${month}-${day}`;
    dateInput.setAttribute('max', maxDate);
}

// Preset alert modal
function presentStatusModal(title, message, visualVariant = 'error', postCloseRoute = null) {
    const overlay = document.getElementById('statusModal');
    const box = document.getElementById('modalBox');
    const titleElem = document.getElementById('modalTitle');
    const msgElem = document.getElementById('modalMessage');
    const iconElem = document.getElementById('modalIcon');

    structuralRedirectTargetUrl = postCloseRoute;

    titleElem.textContent = title;
    msgElem.textContent = message;
    iconElem.textContent = (visualVariant === 'success') ? '✓' : '!';

    box.classList.remove('success-variant', 'error-variant');
    box.classList.add(`${visualVariant}-variant`);

    overlay.classList.add('active');
}

function dismissModal() {
    document.getElementById('statusModal').classList.remove('active');
    if (structuralRedirectTargetUrl) {
        window.location.href = structuralRedirectTargetUrl;
    }
}

/***************************************************
 * backend to frontend transaction control functions
 * *************************************************/

// Convert from (YYYY-MM-DD) to (MM-DD-YYYY)
function reformatDate(dateString) {
    const [year, month, day] = dateString.split('-');
    return `${month}-${day}-${year}`;
}

// Authorization processing script engine
async function updateAccessToken() {
    const displayElement = document.getElementById('accessTokenDisplay');
    const displayElementLabel = document.getElementById("accessTokenLabel");
    const tokenBox = document.getElementById('tokenBox');
    let displayValue = "Not Authorized or Unable to Authorize";

    try {
        const response = await fetch("/get_token");

        if (response.ok){
            const data = await response.json();
            displayValue = data.token;

            if (displayValue !== "unauthorized"){
                tokenVerificationSuccessState = true;
                displayElement.classList.add('is-blurred');

                // Adjust design profile to match clean successful token parameters
                tokenBox.style.backgroundColor = "var(--success-bg)";
                tokenBox.style.borderColor = "var(--success-border)";
                displayElementLabel.style.color = "#81c784";
                displayElement.style.color = "var(--success-color)";
            } else {
                displayElement.textContent = displayValue;
                presentStatusModal(
                    "Authentication Failed",
                    "Invalid credentials provided. Dismiss to navigate to previous page.",
                    "error",
                    "/"
                );
            }
        } else {
            displayElement.textContent = displayValue;
            presentStatusModal(
                "Authentication Failed",
                "An unexpected error occurred. Please try again.",
                "error",
                "/"
            );
        }
    } catch (error) {
        console.error("Connection Failure:", error);
        presentStatusModal(
            "Connection Error",
            "An unexpected error occurred. Please try again.",
            "error",
            "/"
        );
    } finally {
        displayElement.textContent = displayValue;
    }
}

async function handleFormSubmit(event) {
    event.preventDefault();

    const btn = document.getElementById('submitBtn');
    btn.classList.add('loading');
    btn.textContent = 'Sending Request...';

    const taskName = document.getElementById('taskName').value.trim();
    const lat = parseFloat(document.getElementById('latitude').value);
    const lng = parseFloat(document.getElementById('longitude').value);
    const rawDate = document.getElementById('targetDate').value;

    try {
        const response = await fetch('/prepare_task', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                taskName: taskName,
                latitude: lat,
                longitude: lng,
                date: reformatDate(rawDate)
            })
        });

        if (response.ok) {
            const data = await response.json();

            if (data.status === "success") {
                window.location.href = "/success_page";
            } else if (data.status === "fail") {
                window.location.href = "/fail_page";
            } else {
                presentStatusModal(
                    "Submission Error",
                    "The server returned an unrecognized response. Please try again..",
                    "error"
                );
            }
        } else {
            presentStatusModal(
                "Submission Error",
                "An unexpected error occurred trying to proceed to next page. Please try again",
                "error"
            );
        }

    } catch (error) {
        console.error("Transmission Failure:", error);
        presentStatusModal(
            "Submission Error",
            "An unexpected error occurred. Please try again.",
            "error"
        );
    } finally {
        btn.classList.remove('loading');
        btn.textContent = 'Submit Data';
    }
}

restrictFutureDates();
updateAccessToken();