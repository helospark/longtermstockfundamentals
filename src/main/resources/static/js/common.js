  function createTableHtml(columns, rows, dark) {
      if (dark == true) {
        cssClass = "table-dark";
      } else {
        cssClass = "";
      }
      resultHtml = "";
        resultHtml += `
      <table class="table table-striped ` + cssClass + `">
        <thead>
          <tr>`;
      for (i = 0; i < columns.length; ++i) {
         resultHtml += '<th scope="col">' + columns[i] +'</th>';
      }
      resultHtml += `
          </tr>
        </thead>
        <tbody>`;
      for (i = 0; i < rows.length; ++i) {
        resultHtml += "<tr>";
        for (j = 0; j < columns.length; ++j) {
           var value = rows[i][columns[j]];
           resultHtml += "<td>" + value + "</td>";
        }
        resultHtml += "</tr>";
      }
      resultHtml += `
         </tbody>
       </table>`;
       return resultHtml;
  }
  
  function addToWatchlist() {
      $("#generic-modal .modal-content").html(`
          <div class="modal-header">
              <h5 class="modal-title">Add to watchlist</h5>
          </div>
          <div class="modal-body" id="modal-body">
              <label for="watchlist-target-price" class="col-form-label">Target price</label>
              <input type="number" class="form-control" id="watchlist-target-price">
              <label for="watchlist-notes" class="col-form-label">Notes</label>
              <input type="text" class="form-control" id="watchlist-notes">
              <label for="watchlist-tags" class="col-form-label">Comma separated tags</label>
              <input type="text" class="form-control" id="watchlist-tags">
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
            <button type="button" class="btn btn-primary" onclick="addToWatchlistExec()">Save</button>
          </div>`);
          
      modal = new bootstrap.Modal(document.getElementById("generic-modal"));
      modal.show();
  }

  function addToWatchlistExec() {
    var stock = document.getElementById("stock").innerText;
    priceTarget = $("#watchlist-target-price").val();
    notes = $("#watchlist-notes").val();
    tags = $("#watchlist-tags").val().split(",");

    $("#generic-modal").modal("hide");

    fetch('/watchlist', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({symbol: stock, priceTarget: priceTarget, notes: notes, tags: tags})
    }).then(data => {
      console.log(data);

    });
    
  }