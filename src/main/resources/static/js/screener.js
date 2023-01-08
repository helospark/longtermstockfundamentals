  function addNewCondition() {
    newRow = $("#conditions .condition-row").first().clone();
    if ($("#conditions .condition-row").length < 20) {
      $("#conditions").append(newRow);
    }
  }
  
  function submitScreener() {
    exchanges = $("#exchanges").val();
    screenerOperations = collectOperations();
    
    res = fetch('/screener/perform', {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({operations: screenerOperations, exchanges: exchanges})
        }).then(res => {
          return res.json();
        }).then(data => {
            resultHtml = "";
            if (data.errorMessage != undefined && data.errorMessage != null && data.errorMessage.length > 0) {
                $(".screener-error-message").remove();
                $("#screener-result-table").prepend( "<div class=\"screener-error-message\" style=\"color:red\">Error: " + data.errorMessage + "</div>");
            } else {
                columns = data.columns;
                rows = data.portfolio;
            
                resultHtml = createTableHtml(columns, rows, false);

                $("#screener-result-table").html(resultHtml);
                $('#screener-result-table table').DataTable({
                  "iDisplayLength": 100,
                  "order": []
                });
                if (data.hasMoreResults) {
                  $("#screener-result-table").append("<div class=\"screener-more-results\">* there are more results, filter more to see those</div>");
                }
            }
        });
  }
  
  
  function collectOperations() {
    screenerOperations = [];
    $('.condition-row').each(function(i, obj) {
        metric = $(this).find(".metric-dropdown").val();
        operation = $(this).find(".operator-dropdown").val();
        number1 = $(this).find("input:eq(0)").val();
        number2 = 0.0;
        secondField = $(this).find("input:eq(1)");
        if (secondField.length > 0) {
          number2 = secondField.val();
        }
        
        screenerOperations.push({id: metric, operation: operation, number1: number1, number2: number2});
    });
    return screenerOperations;
  }
  
  function submitBacktest() {
    $("#backtest-button").prop("disabled",true);
    $("#backtest-button span").css({display: 'inline-block'});
    
    exchanges = $("#exchanges").val();
    screenerOperations = collectOperations();
    
    startYear = $("#start-date-dropdown").val();
    endYear = $("#end-date-dropdown").val();

    res = fetch('/screener/backtest', {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({operations: screenerOperations, exchanges: exchanges, startYear: startYear, endYear: endYear})
        }).then(res => {
          return res.json();
        }).then(data => {
          $("#backtest-button").prop("disabled",false);
          $("#backtest-button span").css({display: 'none'});
          var canvas = document.createElement("canvas");
          canvas.style='width:100%;max-height:400px'
          
          $("#backtest-result").html($(canvas));
          //canvas.scrollIntoView();
          
          if (!data.investedInAllMatching) {
            $("#backtest-result").prepend("Warning: your conditions matched more than 100 stocks in some years, in those years just 100 matching stocks is invested it (randomly), which will cause variations between runs. Add more filtering conditions to decrease the number of matching stocks.");
          }
          $("#backtest-result").append(`
              <table class="table table-dark">
                <thead>
                  <tr>
                    <th scope="col"></th>
                    <th scope="col">Median annual return</th>
                    <th scope="col">Avg annual return</th>
                    <th scope="col">Invested $</th>
                    <th scope="col">Returned $</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <th scope="row">Screener</th>
                    <td>` + data.screenerMedianPercent.toLocaleString('en-US', {maximumFractionDigits: 2}) + `%</td>
                    <td>` + data.screenerAvgPercent.toLocaleString('en-US', {maximumFractionDigits: 2}) + `%</td>
                    <td>$` + data.investedAmount.toLocaleString('en-US', {maximumFractionDigits: 0}) + `</td>
                    <td>$` + data.screenerReturned.toLocaleString('en-US', {maximumFractionDigits: 0}) + `</td>
                  </tr>
                  <tr>
                    <th scope="row">S&P500</th>
                    <td>` + data.sp500MedianPercent.toLocaleString('en-US', {maximumFractionDigits: 2}) + `%</td>
                    <td>` + data.sp500AvgPercent.toLocaleString('en-US', {maximumFractionDigits: 2})  + `%</td>
                    <td>$` + data.investedAmount.toLocaleString('en-US', {maximumFractionDigits: 0}) + `</td>
                    <td>$` + data.sp500Returned.toLocaleString('en-US', {maximumFractionDigits: 0}) + `</td>
                  </tr>
                </tbody>
              </table>
          `);
          
          var xValues = [];
          var spYValues = [];
          var screenerYValues = [];
          
          console.log(data.yearData);
          for (let [key, value] of Object.entries(data.yearData)) {
            xValues.push(key);
            spYValues.push(value.spAnnualReturnPercent);
            screenerYValues.push(value.screenerAnnualReturnPercent);
            console.log(key + " = " + value);
          }

          colorPalette = ["rgba(0,0,255,0.6)", "rgba(255,0,0,0.6)"]
          colorPaletteLine =  [ "rgba(0,0,255,0.8)", "rgba(255,0,0,0.8)" ]
          
          var chartConfig = {
            type: 'bar',
            data: {
              labels: xValues,
              datasets: [{
                fill: true,
                pointRadius: 2,
                borderColor: colorPaletteLine[0],
                backgroundColor: colorPalette[0],
                data: screenerYValues,
                pointHitRadius: 300,
                label: 'Screener annual return'
              },{
                fill: true,
                pointRadius: 2,
                borderColor: colorPaletteLine[1],
                backgroundColor: colorPalette[1],
                data: spYValues,
                pointHitRadius: 300,
                label: 'S&P 500 annual return'
              }]
            }, 
            options: {
              animation: true,
              responsive: true,
        
               label: {
                 display: false
               },
              plugins: {
                legend: {
                  display: 'S&P500 vs screener'
                },
                tooltip: {
                    callbacks: {
                        label: (item) =>
                            `${item.dataset.label}: ${item.formattedValue}%`,
                    },
                },
              },
              scales: {
                x: {
                  display: true,
                },
                y: {
                  display: true,
                  type: 'linear'
                }
              },
            },
          };
          
          var chart = new Chart(canvas, chartConfig);
          
          yearInvestmentHtml = "";
          for (let [key, value] of Object.entries(data.yearData)) {
             console.log("ASD: " + key + " " + value.investedStocks);
             yearInvestmentHtml += "<h2>" + key + "</h2>";
             yearInvestmentHtml +=  createTableHtml(data.columns, value.investedStocks, true);
          }
          
          fullInvestmentHtml = `
              <div class="accordion" id="backtest-all-invesment-accordion">
                <div class="accordion-item">
                  <h2 class="accordion-header" id="headingOne">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
                      Show all backtest investments
                    </button>
                  </h2>
                  <div id="collapseOne" class="accordion-collapse collapse" aria-labelledby="headingOne" data-bs-parent="#backtest-all-invesment-accordion">
                    <div class="accordion-body panel-collapse">
                      <div>` + yearInvestmentHtml + `</div>
                    </div>
                  </div>
                </div>
              </div>
          
          `;
          $("#backtest-result").append(fullInvestmentHtml);
        });
  }

  function saveScreener() {

    $("#generic-modal .modal-content").html(`
        <div class="modal-header">
            <h5 class="modal-title">Save screener</h5>
        </div>
        <div class="modal-body" id="modal-body">
          <div class="mb-3">
            <label for="screener-name" class="col-form-label">Save as:</label>
            <input type="text" class="form-control" id="screener-name">
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
          <button type="button" class="btn btn-primary" onclick="saveScreenerWithName()">Save</button>
        </div>`);
    $("#generic-modal").modal("show");
  }
  

  function loadScreener() {
    var screenersString = localStorage.getItem('screeners');
    
    if (screenersString === undefined || screenersString === null) {
      screenersToLoad = [];
    } else {
      screenersToLoad = JSON.parse(screenersString);
    }
    
    options = "<select id=\"screener-name\">";
    for (let [key, value] of Object.entries(screenersToLoad)) {
      options += "<option value='" + key + "'>" + key + "</option>";
    }
    options += "</select>"
    
    console.log(options);
  
    $("#generic-modal .modal-content").html(`
        <div class="modal-header">
            <h5 class="modal-title">Load screener</h5>
        </div>
        <div class="modal-body" id="modal-body">
          <div class="mb-3">
            <label for="screener-name" class="col-form-label">Load screener:</label>
            ` + options + `
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
          <button type="button" class="btn btn-primary" onclick="loadScreenerWithName($('#screener-name').val())">Load</button>
        </div>`);
    $("#generic-modal").modal("show");
  }
  
  function saveScreenerWithName() {
    exchanges = $("#exchanges").val();
    screenerOperations = collectOperations();
    
    name = $("#screener-name").val();
    
    
    
    if (name.length < 1) {
      showErrorDialog("Cannot save screener with no name");
      return;
    }
    if (!name.match(/^[_, a-zA-Z0-9]+$/)) {
      showErrorDialog("Please use just letters, numbers, spaces and _ for the name of the screener");
      return;
    }
    
    console.log(name + " " + JSON.stringify({operations: screenerOperations, exchanges: exchanges}));
    
    var screenersString = localStorage.getItem('screeners');
    
    console.log(screenersString);
    if (screenersString === undefined || screenersString === null) {
      screenersToSave = {};
    } else {
      screenersToSave = JSON.parse(screenersString);
    }
    
    result = screenersToSave[name];
    
    screenersToSave[name] = {operations: screenerOperations, exchanges: exchanges};
    
    console.log("TOSAVE: " + screenersToSave);

    localStorage.setItem('screeners', JSON.stringify(screenersToSave));
    localStorage.setItem('last-loaded-screener', name);
    
    $("#generic-modal").modal("hide");
  }
  
  function loadScreenerWithName(name) {
    
    if (name.length < 1) {
      showErrorDialog("Cannot load screener with no name");
      return;
    }
    
    
    var screenersString = localStorage.getItem('screeners');
    
    console.log(screenersString);
    if (screenersString === undefined || screenersString === null) {
      showErrorDialog("Cannot find screener with that name");
      return;
    }
    screenersToSave = JSON.parse(screenersString);
    
    result = screenersToSave[name];
    console.log("NAME= " + name);
    if (result === undefined || result === null) {
      showErrorDialog("Cannot find screener with that name");
      return;
    }
    
    localStorage.setItem('last-loaded-screener', name);
    createScreeners(result);
    
    $("#generic-modal").modal("hide");
  }
  
  function createScreeners(data) {
    if (data.operations === undefined || data.operations == null || data.operations.length == 0) {
      return;
    }
  
    templateRow = $("#conditions .condition-row").first().clone();
    $("#conditions .condition-row").remove();
    
    for (i = 0; i < data.operations.length; ++i) {
      newRow = templateRow.clone();
      newRow.find(".metric-dropdown").val(data.operations[i].id);
      newRow.find(".operator-dropdown").val(data.operations[i].operation);
      newRow.find("input:eq(0)").val(data.operations[i].number1);
      $("#conditions").append(newRow);
    }
    
    console.log("EXCHANGES=" + data.exchanges[1]);
    $("#exchanges").val(data.exchanges);
    
    submitScreener();
  }

  function showErrorDialog(errorMessage) {
    $("#generic-modal .modal-content").html(`
        <div class="modal-header">
            <h5 class="modal-title">Error</h5>
        </div>
        <div class="modal-body" id="modal-body">
          <div class="mb-3">
            ` + errorMessage + `
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
        </div>`);
    $("#generic-modal").modal("show");
}


$( document ).ready(function() {

$('#conditions').on("click", ".dropdown-item",  function(e){
    button = $(this).parent().parent().find("button").first();
    button.text(this.text);
    button.data("metric-id", $(this).data("metric-id"));
});
$('#conditions').on("click", ".remove-filter",  function(e){
    if ($(".condition-row").length > 1) {
      button = $(this).parent().remove();
    }
});

$(".metric-dropdown").val("roic");
$(".operator-dropdown").val(">");
$("#conditions input").first().val("30");


var runInit = false;
if (typeof localStorage !== 'undefined') {
  lastLoadedScreener = localStorage.getItem('last-loaded-screener');
  if (lastLoadedScreener != null) {
    var screenersString = localStorage.getItem('screeners');
    if (!(screenersString === undefined || screenersString === null)) {
      screenersToSave = JSON.parse(screenersString);
      if (screenersToSave[lastLoadedScreener] !== undefined && lastLoadedScreener[lastLoadedScreener] !== null) {
        console.log(lastLoadedScreener);
        loadScreenerWithName(lastLoadedScreener);
        runInit = true;
      }
    }
  }
}
if (!runInit) {
  submitScreener();
}
});

