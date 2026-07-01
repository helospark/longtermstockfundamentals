let gameModal;

function openConfigModal() {
    const settings = getGameSettings();

    const formHtml = `
        <div class="modal-header">
            <h5 class="modal-title">Game Settings</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <form id="configGameForm" onsubmit="saveGameSettings(event)">
            <div class="modal-body">
                <div class="row g-3">
                    <div class="col-6">
                        <label class="form-label">Start Year</label>
                        <input type="number" class="form-control" id="modalStartYear" value="${settings.startYear}" min="2011" max="2026" required>
                    </div>
                    <div class="col-6">
                        <label class="form-label">End Year</label>
                        <input type="number" class="form-control" id="modalEndYear" value="${settings.endYear}" min="2011" max="2026" required>
                    </div>
                    <div class="col-12">
                        <label class="form-label">Universe Selection</label>
                        <select class="form-select" id="modalSelection">
                            <option value="SP500" ${settings.selection === 'SP500' ? 'selected' : ''}>S&P 500</option>
                            <option value="NASDAQ_NYSE" ${settings.selection === 'NASDAQ_NYSE' ? 'selected' : ''}>Nasdaq & NYSE</option>
                            <option value="US_HEADQUARTER" ${settings.selection === 'US_HEADQUARTER' ? 'selected' : ''}>All US headquartered Stocks</option>
                            <option value="US" ${settings.selection === 'US' ? 'selected' : ''}>All US listed Stocks</option>
                            <option value="ALL" ${settings.selection === 'ALL' ? 'selected' : ''}>All Equities</option>
                        </select>
                    </div>
                    <div class="col-6">
                        <label class="form-label">Min Market Cap ($ Billions)</label>
                        <input type="number" step="0.1" class="form-control" id="modalMinCap" value="${settings.minMarketCap}" min="0" max="10" required>
                    </div>
                    <div class="col-6">
                        <label class="form-label">Revenue growth CAGR</label>
                        <input type="number" step="0.1" class="form-control" id="revenueGrowth" value="${settings.revenueGrowth}" min="0" max="30" required>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-primary">Save & Apply</button>
            </div>
        </form>
    `;

    $("#generic-modal .modal-content").html(formHtml);
    
    gameModal = new bootstrap.Modal(document.getElementById("generic-modal"));
    gameModal.show();
}

function saveGameSettings(event) {
    event.preventDefault(); // Stop native postbacks

    const startYear = document.getElementById("modalStartYear").value;
    const endYear = document.getElementById("modalEndYear").value;
    const selection = document.getElementById("modalSelection").value;
    const minMarketCap = document.getElementById("modalMinCap").value;
    const revenueGrowth = document.getElementById("revenueGrowth").value;

    localStorage.setItem("sg_startYear", startYear);
    localStorage.setItem("sg_endYear", endYear);
    localStorage.setItem("sg_selection", selection);
    localStorage.setItem("sg_minMarketCap", minMarketCap);
    localStorage.setItem("sg_revenueGrowth", revenueGrowth);

    if (gameModal) {
        gameModal.hide();
    }

    updateNextButtonUrl();

    const nextBtn = document.getElementById("next-game-btn");
    if (nextBtn) {
        window.location.href = nextBtn.href;
    }
}