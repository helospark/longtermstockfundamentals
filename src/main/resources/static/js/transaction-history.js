function showPortfolioTransactionsModal() {
    $.ajax({
        url: '/portfolio-transactions',
        method: 'GET',
        dataType: 'json',
        success: function(transactions) {
            renderTransactionsModal(transactions || []);
        },
        error: function(xhr, status, error) {
            console.error("Failed to fetch portfolio transactions:", error);
            alert("Unable to load transaction history. Please try again.");
        }
    });
}

function formatCurrencyValue(val, currencyCode) {
    if (val === null || val === undefined || isNaN(val)) return '-';
    
    const num = parseFloat(val);
    const currency = (currencyCode || 'USD').trim().toUpperCase();

    try {
        // 'en-US' guarantees standard left-to-right code placement ("HUF 1" vs localized symbol)
        const formatter = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: currency,
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });

        // Formats as "HUF 1.00" -> Abs value removes any standard negative signs for clean prefixing
        const formatted = formatter.format(Math.abs(num));

        if (num > 0) {
            return `<span class="text-success fw-bold">+${formatted}</span>`;
        } else if (num < 0) {
            return `<span class="text-danger fw-bold">-${formatted}</span>`;
        }
        return `<span>${formatted}</span>`;
    } catch (e) {
        // Fallback safety net for invalid/custom currency codes
        const sign = num > 0 ? '+' : (num < 0 ? '-' : '');
        const colorClass = num > 0 ? 'text-success fw-bold' : (num < 0 ? 'text-danger fw-bold' : '');
        return `<span class="${colorClass}">${sign}${currency} ${Math.abs(num).toFixed(2)}</span>`;
    }
}

function renderTransactionsModal(transactions) {
    let html = `
        <div class="modal-header">
            <h5 class="modal-title">Portfolio Transaction History</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
            <div class="table-responsive">
                <table id="transactionsTable" class="table table-striped table-hover align-middle w-100">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Symbol</th>
                            <th class="text-end">Price</th>
                            <th class="text-end">Amount Change</th>
                            <th class="text-end">Value</th>
                            <th class="text-end">Value (USD)</th>
                        </tr>
                    </thead>
                    <tbody>`;

    transactions.forEach(function(tx) {
        const formattedDate = tx.transactionDateTime 
            ? tx.transactionDateTime.replace('T', ' ').substring(0, 16) 
            : '-';

        function formatSignedValue(val) {
            if (val === null || val === undefined) return '-';
            const num = parseFloat(val);
            const formatted = num.toFixed(2);
            if (num > 0) {
                return `<span class="text-success fw-bold">+${formatted}</span>`;
            } else if (num < 0) {
                return `<span class="text-danger fw-bold">${formatted}</span>`;
            }
            return `<span>0.00</span>`;
        }

        const amountFormatted = tx.amountChange !== null && tx.amountChange !== undefined
            ? (tx.amountChange > 0 ? `+${tx.amountChange}` : tx.amountChange)
            : '-';

        html += `
            <tr>
                <td>${formattedDate}</td>
                <td class="fw-bold">${tx.symbol || '-'}</td>
                <td class="text-end">${tx.sharePrice.toFixed(2)}</td>
                <td class="text-end">${amountFormatted}</td>
                <td class="text-end">${formatCurrencyValue(tx.transactionValue, tx.currency)}</td>
                <td class="text-end">${formatCurrencyValue(tx.transactionValueUsd, 'USD')}</td>
            </tr>`;
    });

    html += `
                    </tbody>
                </table>
            </div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
        </div>`;

    const $modalElem = $('#generic-large-modal');
    $modalElem.find('.modal-content').html(html);

    const modal = new bootstrap.Modal(document.getElementById('generic-large-modal'));

    const table = $('#transactionsTable').DataTable({
        scrollY: '400px',
        scrollCollapse: true,
        paging: false,
        searching: true,
        info: true,
        order: [[0, 'desc']] // Sort newest transactions first by default
    });

    modal.show();

    $modalElem.off('shown.bs.modal').on('shown.bs.modal', function () {
        table.columns.adjust().draw();
    });
}