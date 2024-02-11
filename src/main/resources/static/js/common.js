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
  
  function removeFromWatchlist(stock) {
    formHtml = `
        <div class="modal-header">
            <h5 class="modal-title">Remove ` + stock + ` from watchlist</h5>
        </div>
        <div class="modal-body" id="modal-body">
            <div id="watchlist-error-message" style="color:red;text-weight:600;"></div>
            Are you sure you want to remove this stock from your watchlist?
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-dismiss="modal" data-bs-dismiss="modal">Cancel</button>
          <button type="button" class="btn btn-danger" onclick="removeFromWatchlistExec('` + stock + `')">Remove</button>
        </div>`;
    
    $("#generic-modal .modal-content").html(formHtml);
        
    modal = new bootstrap.Modal(document.getElementById("generic-modal"));
    modal.show();
  }

  function removeFromWatchlistExec(stock) {
    fetch('/watchlist', {
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({symbol: stock})
    }).then(async data => {
        if (data.status != 200) {
          data = await data.json();
          $("#watchlist-error-message").text(data.errorMessage);
        } else {
          $("#generic-modal").modal("hide");
          firstColumn = $("#watchlist-table table tr td:first-child");
          
          for (i = 0; i < firstColumn.length; ++i) {
            if (firstColumn.get(i).textContent == stock) {
              break;
            }
          }
          
          $("#watchlist-table table").get(0).deleteRow(i + 1);
      }
    });
  }

  function createWatchlistTableHtml(columns, rows, dark) {
      if (dark == true) {
        cssClass = "table-dark";
      } else {
        cssClass = "";
      }
      resultHtml = "";
        resultHtml += `
      <table style="width:100%"  class="table table-striped ` + cssClass + `">
        <thead>
          <tr>`;
      for (i = 0; i < columns.length; ++i) {
         resultHtml += '<th scope="col">' + columns[i] +'</th>';
      }
      resultHtml += '<th scope="col" class="watchlist-actions-cell-header">Action</th>';
      resultHtml += `
          </tr>
        </thead>
        <tbody>`;
      for (i = 0; i < rows.length; ++i) {
        resultHtml += "<tr>";
        symbol="";
        for (j = 0; j < columns.length; ++j) {
           var value = rows[i][columns[j]];
           if (columns[j] === "Symbol") {
              symbol = rows[i][columns[j]];
              value = '<a href="/stock/' + symbol + '">' + symbol + '</a>';
           }
           resultHtml += "<td>" + value + "</td>";
        }
        calculatorLink = rows[i]["CALCULATOR_URI"];
        resultHtml += `<td class="watchlist-actions-cell">`;
        if (calculatorLink != null) {
           resultHtml += `<a href="` + calculatorLink + `" class="fa-solid fa-calculator"></a>`;
        }
        resultHtml += `
           <i class="fa-solid fa-pen-to-square" onclick="addToWatchlistWithStock('` + symbol + `')"></i>
           <i class="fa-solid fa-trash" onclick="removeFromWatchlist('` + symbol + `')"></i>
        </td>`;
        resultHtml += "</tr>";
      }
      resultHtml += `
         </tbody>
       </table>`;
       return resultHtml;
  }


  function addToWatchlist() {
      var stock = document.getElementById("stock").innerText;
      
      addToWatchlistWithStock(stock);
  }

  function addToWatchlistWithStock(stock) {
    fetch('/watchlist/' + stock, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    }).then(data => data.json())
    .then(data => {
          fairValueSpan = $("#fair-value");
          var onCalculator = false;
          
          targetPrice = "";
          notes = "";
          tags = "";
          ownedShares = 0;
          if (data.targetPrice != null) {
            targetPrice = data.targetPrice;
          }
          if (data.notes != null) {
            notes = data.notes;
          }
          if (data.tags != null) {
            for (i = 0; i < data.tags.length; ++i) {
              tags += data.tags[i];
              if (i < data.tags.length -1) {
                tags += ", ";
              }
            }
          }
          if (data.ownedShares != null) {
            ownedShares = data.ownedShares;
          }
          
          if (fairValueSpan.length > 0) {
            targetPrice = fairValueSpan.text();
            onCalculator = true;
          }
          
          formHtml = "";
          formHtml = `
              <div class="modal-header">
                  <h5 class="modal-title">Add to watchlist</h5>
              </div>
              <div class="modal-body" id="modal-body">
                  <div id="watchlist-error-message" style="color:red;text-weight:600;"></div>
                  <label for="watchlist-target-price" class="col-form-label">Target price</label>
                  <input type="number" class="form-control" id="watchlist-target-price" value="` + targetPrice + `">
                  <label for="watchlist-notes" class="col-form-label" maxlength="250">Notes</label>
                  <input type="text" class="form-control" id="watchlist-notes" value="` + notes + `">
                  <label for="watchlist-tags" class="col-form-label" maxlength="150">Tags (comma separated):</label>
                  <input type="text" class="form-control" id="watchlist-tags" value="` + tags + `">
                  <label for="watchlist-shares" class="col-form-label" maxlength="7">Owned shares</label>
                  <input type="text" class="form-control" id="watchlist-shares" value="` + ownedShares + `">
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal" data-bs-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary" onclick="addToWatchlistExec('` + stock + `')">Save</button>
              </div>`;
          
          $("#generic-modal .modal-content").html(formHtml);
              
          modal = new bootstrap.Modal(document.getElementById("generic-modal"));
          modal.show();
    });
  }

  function addToWatchlistExec(stock) {
    priceTarget = $("#watchlist-target-price").val();
    notes = $("#watchlist-notes").val();
    tags = $("#watchlist-tags").val().split(",");
    ownedShares = $("#watchlist-shares").val();
    
    const currentUrl = new URL(window.location.href);
    const searchParams = new URLSearchParams(currentUrl.search);
    const type = searchParams.get("type");

    fairValueSpan = $("#fair-value");
    var onCalculator = false;
    if (fairValueSpan.length > 0) {
      startGrowth = Number($("#startGrowth").val());
      endGrowth = Number($("#endGrowth").val());
      startMargin = Number($("#startMargin").val());
      endMargin = Number($("#endMargin").val());
      startShareChange = Number($("#shareChange").val());
      endShareChange = Number($("#endShareChange").val());
      discount = Number($("#discount").val());
      endMultiple = Number($("#endMultiple").val());
      currentPrice = Number($("#current-price").text());
      startPayout = Number($("#startPayout").val());
      endPayout = Number($("#endPayout").text());
       
      calculatorParameters = {
        startMargin: startMargin,
        endMargin: endMargin,
        
        startGrowth: startGrowth,
        endGrowth: endGrowth,
        
        startShChange: startShareChange,
        endShChange: endShareChange,
        
        discount: discount,
        endMultiple: endMultiple,
        
        startPayout: startPayout,
        endPayout: endPayout,
        
        type: type
      };
    } else {
      calculatorParameters = null;
    }

    fetch('/watchlist', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({symbol: stock, priceTarget: priceTarget, notes: notes, tags: tags, ownedShares: ownedShares, calculatorParameters: calculatorParameters})
    }).then(async data => {
        if (data.status != 200) {
          data = await data.json();
          $("#watchlist-error-message").text(data.errorMessage);
        } else {
          $("#generic-modal").modal("hide");
          if ($("#watchlist-table").length > 0) {
            location.reload();
          }
        }
    });
    
  }
  
  
  async function refreshJwt() {
    result = await fetch('/user/jwt/refresh', {
                method: 'POST',
                headers: {
                  'Accept': 'application/json',
                  'Content-Type': 'application/json'
                }
              });
     
     if (result.status != 200) {
       console.log("[ERROR] Unable to refresh JWT");
       return 60000; // try again in 1 minute
     } else {
       expiry = Number(result.headers.get("jwt-expiry")) - 60000;
       if (expiry <= 1000) {
         expiry = 1000;
       }
     }
     return expiry;
}