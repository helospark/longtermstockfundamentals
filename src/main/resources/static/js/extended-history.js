createdCharts = new Map();

function createExtendedHistoryCharts() {
  if (document.getElementById("charts") !== null) {
       $( '<button id="change_cash_button" type="button" class="main_bt btn btn-primary" onclick="changeCash()" style="float:right;margin-right:20px;">+ Cash</button>' ).insertAfter( $( "#DataTables_Table_0_filter" ) );
  
  /*createSeparator("Ratios");
  
      createChart("/historical-performance/pe-ratio", "PE ratio", {
           quarterlyEnabled: false,
           addStockPrefix: false
          });
      createChart("/historical-performance/pfcf-ratio", "PFCF ratio", {
           quarterlyEnabled: false,
           addStockPrefix: false
          });*/
           
  createSeparator("Results");
              
          createChart("/historical-performance/revenue", "Attributable revenue $", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$'
              });
          createChart("/historical-performance/eps", "Attributable EPS", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$'
              });
          createChart("/historical-performance/fcf", "Attributable FCF", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$'
              });
          createChart("/historical-performance/opcash", "Attributable operating cash flow", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$'
              });
          createChart("/historical-performance/equity", "Attributable equity", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$'
              });
          createChart("/historical-performance/cash", "Attributable cash", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$'
              });
          createChart("/historical-performance/total-assets", "Assets and liabilities", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$',
               label: 'Assets',
               additionalCharts: [
               {
                "url": "/historical-performance/total-liabilities",
                "label": "Liabilities"
               }]
          });
          createChart("/historical-performance/total-debt", "Attributable debt", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '$'
              });
          
  createSeparator("Health");
          
              
          createChart("/historical-performance/investment-score", "Investment score", {
               quarterlyEnabled: false,
               addStockPrefix: false
              });
              
      createChart("/historical-performance/altman", "AltmanZ", {
           quarterlyEnabled: false,
           addStockPrefix: false
          });
      createChart("/historical-performance/d2e", "Debt to equity", {
           quarterlyEnabled: false,
           addStockPrefix: false
          });
      createChart("/historical-performance/pietrosky", "Pietrosky", {
           quarterlyEnabled: false,
           addStockPrefix: false
          });
          
          
          
          
          
  createSeparator("Growth");
  
          createChart("/historical-performance/revenue-growth", "Revenue growth", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/eps-growth", "EPS growth", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/fcf-growth", "FCF growth", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/share-change", "2yr share change", {
           quarterlyEnabled: false,
           addStockPrefix: false,
           unit: '%'
          });
              
              
              
              
              
  createSeparator("Return ratios");
              
          createChart("/historical-performance/roic", "ROIC", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/roe", "ROE", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/roa", "ROA", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/fcf-roic", "FCF ROIC", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
              
              
  createSeparator("Margins");
              
              
          createChart("/historical-performance/gross-margin", "Gross margin", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/operating-margin", "Operating margin", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
          createChart("/historical-performance/net-margin", "Net margin", {
               quarterlyEnabled: false,
               addStockPrefix: false,
               unit: '%'
              });
              
              
  createSeparator("Capital allocation");
              
          createChart("/historical-performance/dividends", "Dividends", {
               quarterlyEnabled: false,
               addStockPrefix: false
              });
          createChart("/historical-performance/stock-based-compensation-per-fcf", "Stock based compensation per FCF", {
           quarterlyEnabled: false,
           addStockPrefix: false,
           unit: '%'
          });
          createChart("/historical-performance/stock-based-compensation-per-revenue", "Stock based compensation per revenue", {
           quarterlyEnabled: false,
           addStockPrefix: false,
           unit: '%'
          });
              

    createChart("/historical-performance/rnd_per_ocf", "Usage of operating cash flow", {
      unit: '%',
      label: 'R&D / OCF',
      tooltip: 'Usage of OCF, when the total < 100% extra cash remains, otherwise extra cash used during that quarter',
      stacked: true,
      addStockPrefix: false,
      suggestedMin: 0,
      suggestedMax: 250,
      suggestedMinOfMax: 150,
      guidanceHorizontalLine: {
        yValue: 100,
        lineWidth: 1,
        color: 'red'
      },
      additionalCharts: [
       {
        "url": "/historical-performance/dividend_per_ocf",
        "label": "Dividend / OCF"
       },
       {
        "url": "/historical-performance/buyback_per_ocf",
        "label": "Buyback / OCF"
       },
       {
        "url": "/historical-performance/debt_repayment_per_ocf",
        "label": "Debt repayment / OCF"
       },
       {
        "url": "/historical-performance/capex_per_ocf",
        "label": "CAPEX / OCF"
       },
       {
        "url": "/historical-performance/mna_per_ocf",
        "label": "M&A / OCF"
       }
     ]});
              
              
  }
}
