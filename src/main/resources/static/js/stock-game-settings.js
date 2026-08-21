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



function showScoreboardModal() {
    const rawData = localStorage.getItem('stockGameScoreboard');
    let scores = [];
    
    if (rawData) {
        try {
            scores = JSON.parse(rawData);
        } catch (e) {
            scores = [];
        }
    }

    // 1. Calculate Statistics & Categorization Labels
    let accurateCount = 0;
    let correctCount = 0;
    let omissionCount = 0;
    let commissionCount = 0;
    const diffs = [];

    const processedScores = scores.map((score, index) => {
        const diff = Math.abs(score.guess - score.actual);
        diffs.push(diff);

        let label = { text: 'CORRECT', badge: 'bg-success' };

        if (score.actual > 15 && score.guess < 10) {
            label = { text: 'OMISSION', badge: 'bg-warning text-dark' };
            omissionCount++;
        } else if ((score.actual < -5 && score.guess > 0) || (score.actual < 5 && score.guess > 15)) {
            label = { text: 'COMMISSION', badge: 'bg-danger' };
            commissionCount++;
        } else if (diff <= 3) {
            label = { text: 'ACCURATE', badge: 'bg-primary' };
            accurateCount++;
        } else {
            correctCount++;
        }

        return { ...score, diff, label, originalIndex: index };
    });

    const totalGames = scores.length;
    const rightPercent = totalGames > 0 ? (((accurateCount + correctCount) / totalGames) * 100).toFixed(1) : 0;
    const omissionPercent = totalGames > 0 ? ((omissionCount / totalGames) * 100).toFixed(2) : "0.00";
    const commissionPercent = totalGames > 0 ? ((commissionCount / totalGames) * 100).toFixed(2) : "0.00";
    
    // Median Wrongness calculation
    diffs.sort((a, b) => a - b);
    let medianWrongness = 0;
    if (diffs.length > 0) {
        const mid = Math.floor(diffs.length / 2);
        medianWrongness = (diffs.length % 2 !== 0 ? diffs[mid] : (diffs[mid - 1] + diffs[mid]) / 2).toFixed(2);
    }

    // 2. Generate Modal HTML
    let html = `
        <div class="modal-header">
            <h5 class="modal-title">Performance Scoreboard</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
            <div class="row text-center mb-3 g-2">
                <div class="col-6 col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Right Rate</small>
                        <strong class="fs-5 text-success">${rightPercent}%</strong>
                    </div>
                </div>
                <div class="col-6 col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Median Error</small>
                        <strong class="fs-5">${medianWrongness}%</strong>
                    </div>
                </div>
                <div class="col-6 col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Omissions</small>
                        <strong class="fs-5 text-warning">${omissionPercent}%</strong>
                    </div>
                </div>
                <div class="col-6 col-md-3">
                    <div class="p-2 border rounded bg-light">
                        <small class="text-muted d-block">Commissions</small>
                        <strong class="fs-5 text-danger">${commissionPercent}%</strong>
                    </div>
                </div>
            </div>

            <!-- Middle Section: Scrollable Table -->
            <div class="table-responsive">
                <table id="scoreboardTable" class="table table-striped table-hover align-middle w-100">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Symbol</th>
                            <th class="text-end">Guess</th>
                            <th class="text-end">Actual</th>
                            <th class="text-end">Diff</th>
                            <th class="text-center">Label</th>
                            <th class="text-center">Action</th>
                        </tr>
                    </thead>
                    <tbody>`;

    processedScores.forEach((s) => {
        const formattedDate = s.date ? s.date.split('T')[0] : '';
        html += `
            <tr>
                <td>${formattedDate}</td>
                <td class="fw-bold">${s.symbol}</td>
                <td class="text-end">${s.guess.toFixed(2)}%</td>
                <td class="text-end">${s.actual.toFixed(2)}%</td>
                <td class="text-end">${s.diff.toFixed(2)}%</td>
                <td class="text-center"><span class="badge ${s.label.badge}">${s.label.text}</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-danger delete-row-btn" data-index="${s.originalIndex}">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                </td>
            </tr>`;
    });

    html += `
                    </tbody>
                </table>
            </div>
        </div>
        <!-- Bottom Section: Actions -->
        <div class="modal-footer">
            <button type="button" class="btn btn-danger me-auto" id="clearAllScoresBtn" ${totalGames === 0 ? 'disabled' : ''}>Delete All</button>
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
        </div>`;

    // 3. Inject Content & Render Modal
    $("#very-large-generic-modal .modal-content").html(html);
    const gameModal = new bootstrap.Modal(document.getElementById("very-large-generic-modal"));
    
    // Initialize DataTables for scrolling and ordering
    const table = $('#scoreboardTable').DataTable({
        scrollY: '300px',
        scrollCollapse: true,
        paging: false,
        searching: false,
        info: false,
        order: [[0, 'desc']]
    });

    gameModal.show();

    // Re-adjust DataTables scroll headers when modal fully animates open
    $('#very-large-generic-modal').on('shown.bs.modal', function () {
        table.columns.adjust().draw();
    });

    // 4. Handle Delete Row
    $('#scoreboardTable').on('click', '.delete-row-btn', function () {
        const targetIndex = $(this).data('index');
        scores.splice(targetIndex, 1);
        localStorage.setItem('stockGameScoreboard', JSON.stringify(scores));
        showScoreboardModal(); // Refresh view
    });

    // 5. Handle Delete All
    $('#clearAllScoresBtn').on('click', function () {
        if (confirm("Are you sure you want to delete all saved scores?")) {
            localStorage.removeItem('stockGameScoreboard');
            showScoreboardModal(); // Refresh view
        }
    });
}