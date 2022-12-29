const autoCompleteJS = new autoComplete({
    placeHolder: "Search for stocks...",
    data: {
        src: async (query) => {
          let url = "http://localhost:8080/suggest?search=" + query;
          
          result = [];
          suggestions = await fetch(url)
                          .then(res => res.json())
                          .catch(err => { throw err });
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
                console.log(selection);
            }
        }
    }
});






var stock = document.getElementById("stock").innerText;
var stockToLoad = document.getElementById("stockToLoad").innerText;

function populateProfile() {
  let url = 'http://localhost:8080/' + stockToLoad + "/financials/profile";
  
  fetch(url)
          .then(res => res.json())
          .then(out => {
            console.log("PROFILE");
            console.log(out);
            console.log(out.status);
            if (out.description !== undefined) {
              innerHtml = "";
              innerHtml += "<ul>";
              innerHtml += "<li>Currency: " + out.currency + "</li>";
              innerHtml += "<li>Industry: " + out.sector + " (" + out.industry + ")</li>";
              innerHtml += "<li>Exchange: " + out.exchange + "</li>";
              if (out.website !== undefined && out.website != null) {
                innerHtml += "<li>Website: <a href=\"" + out.website + "\">" + out.website + "</a></li>";
              }
              innerHtml += "</ul>";
              innerHtml += "<div class=\"description-text-div\">" + out.description.replaceAll(/\. ([A-Z])/gm, "\.<br\/>$1") + "</div>";

              document.getElementById("stock-description").innerHTML = innerHtml;
              $("#show-description-link").click(function() {
                $("#stock-description").toggleClass("hidden-element");
              });
            }
          })
          .catch(err => { throw err });
}

function addFlags() {
  let url = 'http://localhost:8080/' + stockToLoad + "/financials/flags";
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

populateProfile();

createSeparator("Income");
createChart("/financials/revenue", "Revenue", {label: "Revenue"});
createChart("/financials/net_income", "Net income", {label: "Net income", additionalCharts: [
  {
    "url": "/financials/fcf",
    "label": "FCF"
  },
]});


createChart("/financials/eps", "EPS", {});
createChart("/financials/pfcf", "FCF per share", {});


createSeparator("Margins")
createChart("/financials/gross_margin", "Gross margin", {suggestedMin: -10, unit: '%', label: "Gross margin", additionalCharts: [
  {
    "url": "/financials/operating_margin",
    "label": "Operating margin"
  }
]});
createChart("/financials/net_margin", "Net margin", {suggestedMin: -20, unit: '%',  label: "Net margin", additionalCharts: [
  {
    "url": "/financials/fcf_margin",
    "label": "FCF margin"
  }
]});


createSeparator("Price ratios")
createChart("/financials/pe_ratio", "PE ratio", {suggestedMin: -5, suggestedMax: 50});
createChart("/financials/cape_ratio", "CAPE ratio", {suggestedMin: -5, suggestedMax: 100});
createChart("/financials/past_pe_to_growth_ratio", "Trailing PEG ratio", {suggestedMin: -2, suggestedMax: 5});
createChart("/financials/ev_over_ebitda", "EV over ebitda", {});

createChart("/financials/p2g_ratio", "Price to Book ratio", {suggestedMin: 0, suggestedMax: 15});
createChart("/financials/quick_ratio", "Quick ratio", {suggestedMin: 0, suggestedMax: 5});


createSeparator("Debt information")

createChart("/financials/cash_flow_to_debt", "Operating cash flow debt coverage", {unit: '%'});
createChart("/financials/short_term_coverage_ratio", "Operating cash flow short term debt coverage", {unit: '%'});
createChart("/financials/short_term_assets_to_total_debt", "Short term assets to total debt", {});
createChart("/financials/altmanz", "Altman Z score", {});
createChart("/financials/interest_expense", "Interest expense", {});
createChart("/financials/interest_rate", "Interest rate", {unit: '%'});
createChart("/financials/interest_coverage", "EBIT / interest", {unit: 'x'});



createSeparator("Assets")
createChart("/financials/current_assets", "Current assets vs liabilities", {label: "Current assets", additionalCharts: [
  {
    "url": "/financials/current_liabilities",
    "label": "Current liabilities"
  }
]});
createChart("/financials/total_assets", "Total assets vs liabilities", {label: "Total assets", additionalCharts: [
  {
    "url": "/financials/total_liabilities",
    "label": "Total liabilities"
  }
]});
createChart("/financials/cash", "Cash and cash equivalents", {});

//createChart("/financials/non_current_assets", "Non current assets", {});
//createChart("/financials/long_term_debt", "Long term debt", {});


createSeparator("Return ratios")
createChart("/financials/roic", "Return on invested capital", {unit: '%'});
createChart("/financials/return_on_assets", "Return on assets", {unit: '%'});
createChart("/financials/return_on_tangible_assets", "Return on tangible assets", {unit: '%'});


createSeparator("Yields")
createChart("/financials/eps_yield", "Earnings flow yield", {suggestedMin: -2, suggestedMax: 50, unit: '%', label: "EPS yield", additionalCharts: [
  {
    "url": "/financials/fcf_yield",
    "label": "Free cash flow yield"
  },
  {
    "url": "/financials/fed_rate",
    "label": "FED funds rate"
  }
]});
createChart("/financials/expected_return", "Compsite yield", {unit: '%'});


createSeparator("Other")
createChart("/financials/share_count", "Share count", {});
createChart("/financials/tax_rate", "Tax rate", {});
createChart("/financials/stock_compensation", "Stock compensation", {unit: '%'});
createChart("/financials/stock_compensation_per_net_income", "Stock compensation / net income", {suggestedMin: -2, unit: '%'});
createChart("/financials/stock_compensation_per_net_revenue", "Stock compensation / revenue", {suggestedMin: 0, unit: '%'});
createChart("/financials/stock_compensation_per_market_cap", "Stock compensation / market cap", {suggestedMin: 0, unit: '%'});
createChart("/financials/capex_to_revenue", "CAPEX to revenue", {unit: '%'});
createChart("/financials/pietrosky_score", "Pietrosky score", {});



createSeparator("Dividend")
createChart("/financials/dividend_yield", "Dividend yield", {unit: '%', suggestedMin: -2, suggestedMax: 50});
createChart("/financials/dividend_payout_ratio", "Dividend payout ratio", {unit: '%', suggestedMin: -2, suggestedMax: 150});
createChart("/financials/dividend_payout_ratio_with_fcf", "Dividend payout ratio FCF", {unit: '%', suggestedMin: -2, suggestedMax: 150});
createChart("/financials/dividend_paid", "Dividend paid", {});
createChart("/financials/dividend_yield_per_current_price", "Dividend yield for invest price", {unit: '%', suggestedMin: -2, suggestedMax: 200});



createSeparator("Growth")
createChart("/financials/eps_growth_rate", "EPS growth annual", {type: 'bar', unit: '%'});
createChart("/financials/eps_growth_rate_7yr_moving_avg", "EPS annual growth x year intervals", {
  type: 'bar',
  unit: '%',
  animation: true,
  tooltip: 'Shows the annual EPS growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized EPS growth between 2013 and 2020.',
  slider: {
    id: "eps_growth_year_slider",
    parameterName: "year",
    min: 2,
    max: 10,
    default: 7
}});

createChart("/financials/fcf_growth_rate", "FCF growth annual", {type: 'bar', unit: '%'});
createChart("/financials/fcf_growth_rate_7yr_moving_avg", "FCF annual growth x year intervals", {
  type: 'bar',
  unit: '%',
  animation: true,
  tooltip: 'Shows the annual FCF/share growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized FCF/share growth between 2013 and 2020.',
  slider: {
    id: "fcf_growth_year_slider",
    parameterName: "year",
    min: 2,
    max: 10,
    default: 7
}});

createChart("/financials/revenue_growth_rate", "Revenue growth annual", {type: 'bar', unit: '%'});
createChart("/financials/share_count_growth_rate", "Share count growth annual", {type: 'bar', unit: '%'});
createChart("/financials/dividend_growth_rate", "Dividend growth annual", {type: 'bar', unit: '%'});



createSeparator("DCF")
createChart("/financials/eps_dcf", "EPS DCF", {});
createChart("/financials/fcf_dcf", "FCF DCF", {});
createChart("/financials/dividend_dcf", "Dividend DCF", {});
createChart("/financials/cash_per_share", "Cash per share", {});
createChart("/financials/graham_number", "Graham number", {});


createSeparator("Price")
createChart("/financials/price", "Price", {label: "price", additionalCharts: [
  {
    "url": "/financials/composite_fair_value",
    "label": "Composite fair value"
  }
]});
createChart("/financials/price_growth_rate", "Price growth", {type: 'bar'});

addFlags();

$(document).ready(function(){
  $('[data-toggle="tooltip"]').tooltip();
});
