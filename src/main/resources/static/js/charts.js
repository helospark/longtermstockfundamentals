newest_timestamp = 0;
const autoCompleteJS = new autoComplete({
    placeHolder: "Search for stocks...",
    data: {
        src: async (query) => {
          sent_timestamp = (new Date()).getTime();
          let url = "/suggest?search=" + query;
          
          suggestions = await fetch(url)
                          .then(res => res.json())
                          .catch(err => { throw err });
          if (sent_timestamp < newest_timestamp) {
              throw Error("Old results");
          }
          newest_timestamp = sent_timestamp;
          result = [];

          for (i = 0; i < suggestions.length; ++i) {
            out = suggestions[i];
            element=out.symbol + " - " + out.name;
            result.push({
               key: out.symbol,
               display: element
            });
          }
          return result;
        },
        keys: ["display"],
        cache: false
    },
    resultItem: {
        highlight: true,
    },
    events: {
        input: {
            selection: (event) => {
                const selection = event.detail.selection.value;
                window.location.href = "/stock/" + selection.key
            }
        }
    }
});



// scroll to top
let mybutton = document.getElementById("scrollTopBtn");

// When the user scrolls down 20px from the top of the document, show the button
window.onscroll = function() {scrollFunction()};

function scrollFunction() {
  if (document.body.scrollTop > 20 || document.documentElement.scrollTop > 20) {
    mybutton.style.display = "block";
  } else {
    mybutton.style.display = "none";
  }
}

// When the user clicks on the button, scroll to the top of the document
function scrollToTop() {
  document.body.scrollTop = 0; // For Safari
  document.documentElement.scrollTop = 0; // For Chrome, Firefox, IE and Opera
}
// scroll to top



var stock = document.getElementById("stock").innerText;
var stockToLoad = document.getElementById("stockToLoad").innerText;

function populateProfile() {
  let url = '/' + stockToLoad + "/financials/profile";
  
  fetch(url)
          .then(res => res.json())
          .then(out => {
            if (out.description !== undefined) {
              symbolText = out.currencySymbol == "" ? "" : "(" + out.currencySymbol + ")";
              innerHtml = "";
              innerHtml += "<ul>";
              innerHtml += "<li>Reporting currency: " + out.reportedCurrency + " " + symbolText + "</li>";
              innerHtml += "<li>Trading currency: " + out.currency + "</li>";
              innerHtml += "<li>Industry: " + out.sector + " (" + out.industry + ")</li>";
              innerHtml += "<li>Exchange: " + out.exchange + "</li>";
              if (out.website !== undefined && out.website != null) {
                innerHtml += "<li>Website: <a href=\"" + out.website + "\">" + out.website + "</a></li>";
              }
              innerHtml += "<li>Yahoo Finance: <a href=\"https://finance.yahoo.com/quote/" + stockToLoad + "\">https://finance.yahoo.com/quote/" + stockToLoad + "</a></li>";
              innerHtml += "</ul>";
              var lines = out.description.split(/\. /gm);
              var description = "";
              for (i = 0; i < lines.length; ++i) {
                 description += lines[i];
                 if (lines[i].length < 20 || lines[i].match(/.*?Inc$/gm) || (i < lines.length - 1 && (!lines[i + 1].match(/[A-Z].*/gm)))) {
                   description += ". ";
                 } else {
                   description += ".<br />";
                 }
              }
              
              innerHtml += "<div class=\"description-text-div\">" + description + "</div>";

              document.getElementById("stock-description").innerHTML = innerHtml;
              $("#show-description-link").click(function() {
                $("#stock-description").toggleClass("hidden-element");
              });
            }
          })
          .catch(err => { console.log(err); });
}

function addFlags() {
  let url = '/' + stockToLoad + "/financials/flags";
  var flagsDiv = document.getElementById("flags");
  fetch(url)
          .then(res => res.json())
          .then(out => {
              for (elementIndex in out) {
                value = out[elementIndex];
                if (value.type == "RED") {
                    flagsDiv.innerHTML += "&#128308; " + value.text + "<br/>";
                } else if (value.type == "GREEN") {
                    flagsDiv.innerHTML += "&#128994; " + value.text + "<br/>";
                } else if (value.type == "STAR") {
                    flagsDiv.innerHTML += "&#11088; " + value.text + "<br/>";
                } else if (value.type == "YELLOW") {
                    flagsDiv.innerHTML += "&#128992; " + value.text + "<br/>";
                }
              }
          })
          .catch(err => { throw err });
}


function createSeparator(title) {
  var hrElement =document.createElement("hr");
  var hone = document.createElement("h3");
  
  hone.innerHTML = title;
  
  var element = document.getElementById("charts");
  
  children=Array.from(element.children);
  
  if (children.length > 1) {
    element.appendChild(hrElement);
  }
  element.appendChild(hone);
  
  
  dropDown = $("#chart-dropdown");
  dropDownElements = $("#chart-dropdown li");
  if (dropDown.length > 0) {
    if (dropDownElements.length > 0) {
      dropDown.append("<div class=\"dropdown-divider\"></div>");
    }
    dropDown.append("<div class=\"dropdown-header\">" + title + "</div>");
  }
  
  
}


allCharts=[]

function findInArrayById(array, id) {
  result = array.filter((element) => element.id === id);
  if (result.length == 1) {
    return result[0];
  } else {
    return null;
  }
}

function findIndexInArrayById(array, id) {
  for (o = 0; o < array.length; ++o) {
    if (array[o].id === id) {
      return o;
    }
  }
  return null;
}

function getCurrentlySavedCustomizedCharts() {
  if (localStorage !== undefined && localStorage.getItem("customizedCharts") !== undefined && localStorage.getItem("customizedCharts") !== null) {
    var result = JSON.parse(localStorage.getItem("customizedCharts"));
    
    for (k = 0; k < allCharts.length; ++k) {
      if (findInArrayById(result, allCharts[k].id) === null) {
        var item = {id: allCharts[k].id, enabled:allCharts[k].defaultEnabled};
        if (k === 0) {
          result.splice(0, 0, item);
        } else {
          for (u = k - 1; u >= 0; --u) {
            var arrayIndex = findInArrayById(result, allCharts[u].id);
            
            if (arrayIndex !== null) {
              break;
            }
          }
          
          if (u >= 0) {
            result.splice(u + 1, 0, item);
          } else {
            result.push(item);
          }
        }
      }
    }
    return result;
  } else {
    var result = [];
    
    for (k = 0; k < allCharts.length; ++k) {
      result.push({id: allCharts[k].id, enabled:allCharts[k].defaultEnabled});
    }
    return result;
  }
}


function customizeCharts() {  
  var modalHtml=`<div>Select and reorder charts</div><hr/><ul id="customizer-list">`;
  
  var currentCharts = getCurrentlySavedCustomizedCharts();
  
  for (let i = 0; i < currentCharts.length; i++) {
    var currentChart = findInArrayById(allCharts, currentCharts[i].id);
    if (currentChart == null) {
      console.log(currentCharts[i].id);
    }
    var currentSetting = currentCharts[i];
    if (currentSetting !== null) {
      var isChecked = currentSetting.enabled;
      modalHtml+=`
          <li>
            <div class="form-check" style="${currentChart.separator && i>0 ? 'margin-top: 30px' : ''}">
              <input class="form-check-input" name="customize-${i}" type="checkbox" value="${currentChart.id}" ${isChecked ? 'checked' : ''}>
              <label style="margin-left:8px" class="form-check-label" for="customize-${i}">
                ${currentChart.title}
              </label>
            </div>
          </li>
      `;
    }
    
  }
  modalHtml+=`</ul>`;
  
  
  
  $("#generic-large-modal .modal-content").html(
  `
      <div class="modal-header">
        <h5 class="modal-title" id="exampleModalScrollableTitle">Customize charts</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        ${modalHtml}
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-danger" data-bs-dismiss="modal" onClick="resetCustomizedData()">Reset</button>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
        <button type="button" class="btn btn-primary" onClick="saveCustomizedData()">Save changes</button>
      </div>`
  
  );
  $("#generic-large-modal").modal("show");
  Sortable.create($("#customizer-list").get(0), {});
}

function saveCustomizedData() {
  var resultMap = [];
  checkboxes = $("#customizer-list input").each(function() {
    resultMap.push({id:$(this).attr("value"), enabled:$(this).is(':checked')});
  });
  console.log("Saving");
  console.log(resultMap);
  
  dataToSave = JSON.stringify(resultMap);
  
  localStorage.setItem("customizedCharts", dataToSave);
  
  $("#generic-large-modal").modal("hide");
  location.reload();
}

function resetCustomizedData() {
  localStorage.removeItem("customizedCharts");
  $("#generic-large-modal").modal("hide");
  location.reload();
}

function renderChart(allCharts, id) {
  const result = allCharts.filter((element)=>element.id === id);
  console.log(result);
  if (result !== undefined && result != null && result.length == 1) {
    result[0].func();
  }
}

function createSeparatorInternal(name, defaultEnabled=true) {
  allCharts.push({title:"<b>" + name + " (title)</b>", id:"__" + name, func:() => createSeparator(name), defaultEnabled: defaultEnabled, separator:true});
}

function createChartInternal(url, title, configs, defaultEnabled = true) {
  allCharts.push({title:title, id:url.replaceAll("/financials/", ""), func:() => createChart(url, title, configs), defaultEnabled: defaultEnabled, separator:false });
} 

populateProfile();

allCharts=[];
createSeparatorInternal("Income");
createChartInternal("/financials/revenue", "Revenue", {label: "Revenue"});
createChartInternal("/financials/net_income", "Net income", {label: "Net income", additionalCharts: [
  {
    "url": "/financials/fcf",
    "label": "FCF"
  },
]});
createChartInternal("/financials/eps", "EPS", {});
createChartInternal("/financials/eps_excl_rnd", "EPS excluding research and development", {
  tooltip: 'Some companies, such as AMZN reinvest most of their earnings via R&D, making EPS meaningless, but we could readd the R&D cost to see what the company could have achieved. Also EPS excluding marketing is shown, if the consumers sticiking to the brand, the marketing could be stopped.',
  label: "EPS excluding R&D",
  additionalCharts: [
  {
    "url": "/financials/eps_excl_marketing",
    "label": "EPS excluding marketing expense"
  }
]}, defaultEnabled=false);
createChartInternal("/financials/pfcf", "FCF per share", {});
createChartInternal("/financials/ebitda_per_share", "EBITDA per share", {}, defaultEnabled=false);


createSeparatorInternal("Margins")
createChartInternal("/financials/gross_margin", "Gross margin", {suggestedMin: -20, unit: '%', label: "Gross margin", additionalCharts: [
  {
    "url": "/financials/operating_margin",
    "label": "Operating margin"
  }
]});
createChartInternal("/financials/net_margin", "Net margin", {suggestedMin: -20, unit: '%',  label: "Net margin", additionalCharts: [
  {
    "url": "/financials/fcf_margin",
    "label": "FCF margin"
  }
]});
createChartInternal("/financials/operating_fcf_margin", "Operating FCF margin", {suggestedMin: -20, unit: '%'}, defaultEnabled=false);
createChartInternal("/financials/operating_ebitda_margin", "EBITDA margin", {suggestedMin: -20, unit: '%'}, defaultEnabled=false);

createSeparatorInternal("Expenses")
createChartInternal("/financials/marketing_per_operating_expense", "Operating expense breakdown", {unit: '%', suggestedMin: 0, suggestedMax: 100, label: 'Marketing expense %', additionalCharts: [
  {
    "url": "/financials/rd_per_operating_expense",
    "label": "Research and development expense %"
  },
  {
    "url": "/financials/admin_per_operating_expense",
    "label": "General administrative expense %"
  },
  {
    "url": "/financials/other_per_operating_expense",
    "label": "Other expense %"
  }
]});

createSeparatorInternal("Price ratios")
createChartInternal("/financials/pe_ratio", "PE ratio", {suggestedMin: -5, suggestedMax: 50, quarterlyEnabled: false});
createChartInternal("/financials/price_to_op_cash_ratio", "Price to operating cash flow ratio", {suggestedMin: -5, suggestedMax: 50, quarterlyEnabled: false});
createChartInternal("/financials/cape_ratio", "CAPE ratio", {suggestedMin: -5, suggestedMax: 100, quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/pfcf_ratio", "Price to FCF ratio", {suggestedMin: -5, suggestedMax: 50, quarterlyEnabled: false});
createChartInternal("/financials/pe_excl_rnd_ratio", "PE ratio excluding", {
  suggestedMin: -5,
  suggestedMax: 50,
  quarterlyEnabled: false,
  label: 'PE ratio excluding R&D', additionalCharts: [
    {
      "url": "/financials/pe_excl_marketing_ratio",
      "label": "PE excluding marketing&sales ratio"
    },
    {
      "url": "/financials/pe_excl_amortization",
      "label": "PE excluding amortization and depreciation"
    }
  ]});
createChartInternal("/financials/price_to_gross_profit", "Price to gross profit ratio", {suggestedMin: -5, suggestedMax: 50, quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/price_to_sales", "Price to sales ratio", {suggestedMin: -5, suggestedMax: 50, quarterlyEnabled: false});
createChartInternal("/financials/accrual_ratio", "Accrual ratio", {
  quarterlyEnabled: false,
  tooltip: 'Small or negative are better. Formula: (netIncome - FCF) / totalAssets',
}, defaultEnabled=false);


createChartInternal("/financials/past_pe_to_growth_ratio", "Trailing PEG ratio", {
  tooltip: 'Trailing PEG is the PE ratio divided by the median past [1..7] year annual EPS growth. Generally above 2 is expensive, below 1 is cheap.',
  suggestedMin: -2,
  suggestedMax: 5,
  quarterlyEnabled: false});


createChartInternal("/financials/past_cape_to_growth_ratio", "Trailing PEG ratio variations", {
  suggestedMin: -2,
  suggestedMax: 5,
  quarterlyEnabled: false,
  label: "CAPEG",
  tooltip: 'Variations of trailing PEG. CAPEG is the 4 year Shiller PE divided by the past [1..7] year annual EPS growth. The trailing revenue PEG is the same as the trailing PEG, but uses revenue growth instead of EPS growth.',
  additionalCharts: [
  {
    "url": "/financials/past_pe_to_rev_growth_ratio",
    "label": "trailing revenue PEG"
  }
]}, defaultEnabled=false);
createChartInternal("/financials/ev_over_ebitda", "EV over ebitda", {});

createChartInternal("/financials/p2b_ratio", "Price to Book ratio", {
  suggestedMin: 0,
  suggestedMax: 15,
  quarterlyEnabled: false,
  label: "price / book value"
});



createSeparatorInternal("Debt information")


createChartInternal("/financials/cash_flow_to_debt", "Operating cash flow debt coverage", {unit: '%', quarterlyEnabled: false});
createChartInternal("/financials/short_term_coverage_ratio", "Operating cash flow short term debt coverage", {unit: '%', quarterlyEnabled: false});
createChartInternal("/financials/short_term_assets_to_total_debt", "Short term assets to total debt", {quarterlyEnabled: false});
createChartInternal("/financials/altmanz", "Altman Z score", {
  quarterlyEnabled: false

});
createChartInternal("/financials/interest_coverage", "EBIT / interest", {unit: 'x'});
createChartInternal("/financials/sloan", "Sloan ratio", {
  quarterlyEnabled: false,
  tooltip: 'Sloan ratio shows if earnings closely match cashflows. Between -10% to 10% is considered safe, outside of -25% to 25% is in danger zone',
  unit: '%'
}, defaultEnabled=false);
createChartInternal("/financials/interest_expense", "Interest expense", {});
createChartInternal("/financials/interest_rate", "Interest rate", {unit: '%', quarterlyEnabled: false});
createChartInternal("/financials/debt_to_equity", "Debt to equity", {
  quarterlyEnabled: false,
  tooltip: 'Lower is better, generally below 1 is healthy, above 2 is risky',
  suggestedMin: -10,
  suggestedMax: 10
});
createChartInternal("/financials/quick_ratio", "Quick ratio", {suggestedMin: 0, suggestedMax: 5, quarterlyEnabled: false});
createChartInternal("/financials/current_ratio", "Current ratio", {suggestedMin: 0, suggestedMax: 5, quarterlyEnabled: false}, defaultEnabled=false);


createSeparatorInternal("Assets")
createChartInternal("/financials/current_assets", "Current assets vs liabilities", {label: "Current assets", quarterlyEnabled: false, additionalCharts: [
  {
    "url": "/financials/current_liabilities",
    "label": "Current liabilities"
  }
]});
createChartInternal("/financials/total_assets", "Total assets vs liabilities", {label: "Total assets", quarterlyEnabled: false, additionalCharts: [
  {
    "url": "/financials/total_liabilities",
    "label": "Total liabilities"
  }
]});
createChartInternal("/financials/intangible_assets_percent", "Intangible assets", {
   unit: '%',
   quarterlyEnabled: false,
   label: "Total intangible assets to total assets",
   additionalCharts: [
   {
     "url": "/financials/goodwill_percent",
     "label": "Goodwill to total assets"
   }
]});
createChartInternal("/financials/cash", "Cash and cash equivalents", {quarterlyEnabled: false});

createChartInternal("/financials/non_current_assets", "Non current assets", {quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/long_term_debt", "Debt", {
  quarterlyEnabled: false, label: "Long term debt",
  additionalCharts: [
  {
    "url": "/financials/short_term_debt",
    "label": "Short term debt"
  }
]});



createSeparatorInternal("Return ratios") 
createChartInternal("/financials/roic", "Return on invested capital", {
  unit: '%',
  quarterlyEnabled: false,
  suggestedMin: -10,
  tooltip: 'Using formula: EBIT / (totalAssets - currentLiabilities)'
});
createChartInternal("/financials/fcf_roic", "Return on invested capital using FCF", {
  unit: '%',
  quarterlyEnabled: false,
  suggestedMin: -10,
  tooltip: 'Using formula: (fcf * (1.0 - taxRate)) / (equity + totalDebt)'
});
createChartInternal("/financials/return_on_assets", "Return on assets", {unit: '%', quarterlyEnabled: false, suggestedMin: -10});
createChartInternal("/financials/return_on_tangible_assets", "Return on tangible assets", {unit: '%', quarterlyEnabled: false, suggestedMin: -10});
createChartInternal("/financials/return_on_tangible_capital", "Return on tangible capital", {
  unit: '%',
  quarterlyEnabled: false,
  suggestedMin: -10,
  tooltip: 'Using formula: EBIT / (workingCapital + fixedAssets)'});
createChartInternal("/financials/return_on_equity", "Return on equity", {unit: '%', quarterlyEnabled: false, suggestedMin: -10});
createChartInternal("/financials/roiic", "Return on incremental invested capital (ROIIC)", {
  unit: '%',
  quarterlyEnabled: false,
  suggestedMin: -100,
  suggestedMax: 100,
  tooltip: 'Using formula: (NOPAT2 - NOPAT1) / (IC2 - IC1)',
  slider: {
    id: "roiic_year_slider",
    parameterName: "year",
    min: 1,
    max: 10,
    default: 7
  }
});


createSeparatorInternal("Yields")
createChartInternal("/financials/eps_yield", "Earnings flow yield", {suggestedMin: -2, suggestedMax: 50, unit: '%', label: "EPS yield", quarterlyEnabled: false,
 additionalCharts: [
  {
    "url": "/financials/fcf_yield",
    "label": "Free cash flow yield"
  },
  {
    "url": "/financials/fed_rate",
    "label": "FED funds rate"
  }
]});
createChartInternal("/financials/expected_return", "Compsite yield", {unit: '%'}, defaultEnabled=false);


createSeparatorInternal("Other")
createChartInternal("/financials/market_cap_usd", "Market cap $", {quarterlyEnabled: false, label: '$'});
createChartInternal("/financials/share_count", "Share count", {quarterlyEnabled: false});
createChartInternal("/financials/stock_compensation", "Stock compensation", {quarterlyEnabled: true});
createChartInternal("/financials/stock_compensation_per_net_income", "Stock compensation / net income", {suggestedMin: -2, unit: '%'});
createChartInternal("/financials/stock_compensation_per_net_revenue", "Stock compensation / revenue", {suggestedMin: 0, unit: '%'});
createChartInternal("/financials/stock_compensation_per_market_cap", "Stock compensation / market cap", {suggestedMin: 0, unit: '%'});
createChartInternal("/financials/capex_to_revenue", "CAPEX to revenue", {unit: '%', label: "CAPEX to revenue",  additionalCharts: [
  {
    "url": "/financials/rnd_to_revenue",
    "label": "R&D to revenue"
  }]
});
createChartInternal("/financials/acquisitions_per_market_cap", "Acquisitions to marketcap", {unit: '%'});
createChartInternal("/financials/asset_turnover_ratio", "Asset turnover ratio", {quarterlyEnabled: true}, defaultEnabled=false);
createChartInternal("/financials/pietrosky_score", "Piotrosky score", {quarterlyEnabled: false});
createChartInternal("/financials/effective_tax_rate", "Effective tax rate", {quarterlyEnabled: true, unit: '%'}, defaultEnabled=false);
//createChartInternal("/financials/earnings_surprise", "Earnings surprise", {type: 'bar', unit: '%', quarterlyEnabled: false});



createSeparatorInternal("Dividend")
createChartInternal("/financials/dividend_yield", "Dividend yield", {unit: '%', suggestedMin: -2, suggestedMax: 50, quarterlyEnabled: false});
createChartInternal("/financials/dividend_payout_ratio", "Dividend payout ratio", {unit: '%', suggestedMin: -2, suggestedMax: 150});
createChartInternal("/financials/dividend_payout_ratio_with_fcf", "Dividend payout ratio FCF", {unit: '%', suggestedMin: -2, suggestedMax: 150});
createChartInternal("/financials/dividend_paid", "Dividend paid", {quarterlyEnabled: false});
createChartInternal("/financials/dividend_yield_per_current_price", "Dividend yield on cost", {unit: '%', suggestedMin: -2, suggestedMax: 200}, defaultEnabled=false);
createChartInternal("/financials/total_dividend_per_share_since", "Cumulative dividends payed per share since", {suggestedMin: -2, suggestedMax: 200}, defaultEnabled=false);
createChartInternal("/financials/share_buyback_per_net_income", "Net share buyback / net income", {
  unit: '%',
  tooltip: 'Positive means shares were bought back, negative means, shares were issued',
});
createChartInternal("/financials/share_buyback_per_net_fcf", "Net share buyback / FCF", {
  unit: '%',
  tooltip: 'Positive means shares were bought back, negative means, shares were issued',
});
createChartInternal("/financials/total_payout_ratio", "Total payout / net income", {
  unit: '%',
  tooltip: 'Total payout (dividend + buyback)',
});


createChartInternal("/financials/rnd_per_ocf", "Usage of operating cash flow", {
  unit: '%',
  label: 'Dividend / OCF',
  tooltip: 'Usage of OCF, when the total < 100% extra cash remains, otherwise extra cash used during that quarter',
  stacked: true,
  suggestedMin: 0,
  suggestedMax: 250,
  additionalCharts: [
   {
    "url": "/financials/dividend_per_ocf",
    "label": "Dividend / OCF"
   },
   {
    "url": "/financials/buyback_per_ocf",
    "label": "Buyback / OCF"
   },
   {
    "url": "/financials/debt_repayment_per_ocf",
    "label": "Debt repayment / OCF"
   },
   {
    "url": "/financials/capex_per_ocf",
    "label": "CAPEX / OCF"
   },
   {
    "url": "/financials/mna_per_ocf",
    "label": "M&A / OCF"
   }
 ]});


createSeparatorInternal("Growth")
createChartInternal("/financials/eps_growth_rate", "EPS growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChartInternal("/financials/eps_growth_rate_7yr_moving_avg", "EPS annual growth x year intervals", {
  type: 'bar',
  unit: '%',
  quarterlyEnabled: false,
  tooltip: 'Shows the annual EPS growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized EPS growth between 2013 and 2020.',
  slider: {
    id: "eps_growth_year_slider",
    parameterName: "year",
    min: 1,
    max: 10,
    default: 7
}});

createChartInternal("/financials/fcf_growth_rate", "FCF growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChartInternal("/financials/fcf_growth_rate_7yr_moving_avg", "FCF annual growth x year intervals", {
  type: 'bar',
  unit: '%',
  quarterlyEnabled: false,
  tooltip: 'Shows the annual FCF/share growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized FCF/share growth between 2013 and 2020.',
  slider: {
    id: "fcf_growth_year_slider",
    parameterName: "year",
    min: 1,
    max: 10,
    default: 7
}});

createChartInternal("/financials/revenue_growth_rate", "Revenue growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false,});

createChartInternal("/financials/revenue_growth_rate_xyr_moving_avg", "Revenue annual growth x year intervals", {
      type: 'bar',
      unit: '%',
      lazyLoad: false,
      quarterlyEnabled: false,
      tooltip: 'Shows the annual revenue growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized revenue growth between 2013 and 2020.',
      slider: {
        id: "revenue_growth_rate_xyr_moving_avg_slider",
        parameterName: "year",
        min: 1,
        max: 10,
        default: 7
}});
createChartInternal("/financials/share_count_growth_rate", "Share count growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChartInternal("/financials/dividend_growth_rate", "Dividend growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});



/*
createSeparatorInternal("Trading")
createChartInternal("/financials/insider_trading_bought", "Insider bought shares", {
     quarterlyEnabled: false,
     type: 'bar',
     label: 'Insider bought shares',
     additionalCharts: [
     {
      "url": "/financials/insider_trading_sold",
      "label": "Insider sold stocks"
     }]
});
createChartInternal("/financials/senate_trading_bought", "Senate bought stocks", {
     quarterlyEnabled: false,
     type: 'bar',
     label: 'Senate bought stocks',
     unit: '$',
     additionalCharts: [
     {
      "url": "/financials/senate_trading_sold",
      "label": "Senate sold stocks"
     }]
});
*/


createSeparatorInternal("EM", defaultEnabled=false)
createChartInternal("/financials/5_year_pe", "5 year PE", {type: 'bar', quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/5_year_pfcf", "5 year price to FCF", {type: 'bar',quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/5_year_rev_growth", "5 year revenue growth", {type: 'bar', unit: '%', quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/5_year_netincome_growth", "5 year net income growth", {type: 'bar', unit: '%', quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/5_year_fcf_growth", "5 year FCF growth", {type: 'bar', unit: '%', quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/5_year_share_growth", "5 year share growth", {type: 'bar', unit: '%', quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/5_year_roic", "5 year ROIC", {type: 'bar', quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/ltl_per_5yr_fcf", "Long term liabilities / 5yr avg FCF", {type: 'bar', quarterlyEnabled: false}, defaultEnabled=false);


createSeparatorInternal("Value")

createChartInternal("/financials/eps_dcf", "EPS DCF", {quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/fcf_dcf", "FCF DCF", {quarterlyEnabled: false}, defaultEnabled=false);
createChartInternal("/financials/dividend_dcf", "Dividend DCF", {quarterlyEnabled: false}, defaultEnabled=false);

createChartInternal("/financials/cash_per_share", "Cash per share", {quarterlyEnabled: false});
createChartInternal("/financials/equity_per_share", "Equity per share", {quarterlyEnabled: false});
createChartInternal("/financials/graham_number", "Graham number", {quarterlyEnabled: false}, defaultEnabled=false);

createChartInternal("/financials/investment_score", "Investment score", {
  quarterlyEnabled: false,
  tooltip: 'Composite score of the most important fundamentals of the company, between 0.0 (worst) and 10.0 (best)',
  suggestedMin: 0,
  suggestedMax: 10
});


createSeparatorInternal("Price")
createChartInternal("/financials/price", "Price vs calculated fair value", {
   label: "price",
   quarterlyEnabled: false,
   tooltip: 'Calculated fair value is the value you get with the default form on the calculator page.',
   additionalCharts: [
  {
    "url": "/financials/default_calculator_result",
    "label": "Calculated fair value"
  }
]});
createChartInternal("/financials/return_with_reinvested_dividend", "Total returns", {
  label: "Returns with reinvested dividends",
  quarterlyEnabled: false,
  tooltip: 'Returns if each dividend received is reinvested into more stocks (assumes no transaction fee or tax)',
  additionalCharts: [{
    "url": "/financials/price",
    "label": "price"
  }
]});
createChartInternal("/financials/price_growth_rate", "Price growth", {type: 'bar', quarterlyEnabled: false, unit: '%'}, defaultEnabled=false);
createChartInternal("/financials/price_with_dividends_growth_rate", "Returns (with dividends reinvested)", {type: 'bar', quarterlyEnabled: false, unit: '%'});
createChartInternal("/financials/price_growth_rate_xyr_moving_avg", "Returns x year interval", {
  type: 'bar',
  quarterlyEnabled: false,
  unit: '%',
  tooltip: 'Shows the total annualized returns every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized returns between 2013 and 2020.',
  slider: {
    id: "eps_growth_year_slider",
    parameterName: "year",
    min: 1,
    max: 10,
    default: 7
  }
});


createChartInternal("/financials/price_growth_rate_xyr_moving_avg_trailing", "PE vs growth", {
  quarterlyEnabled: false,
  tooltip: 'Compares PE with return',
  label: "CAGR",
  suggestedMin: -5,
  suggestedMax: 60,
  slider: {
    id: "eps_growth_year_slider",
    parameterName: "year",
    min: 1,
    max: 25,
    default: 10
  },
  additionalCharts: [{
    "url": "/financials/pe_ratio",
    "label": "pe ratio"
  }]
});

createChartInternal("/financials/detailed_price", "Stock price", {quarterlyEnabled: false});

var currentlySavedCharts = getCurrentlySavedCustomizedCharts();
for (var k=0; k<currentlySavedCharts.length; ++k) {
  if (currentlySavedCharts[k].enabled) {
    renderChart(allCharts, currentlySavedCharts[k].id);
  }
}


addFlags();

var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
  return new bootstrap.Tooltip(tooltipTriggerEl)
})