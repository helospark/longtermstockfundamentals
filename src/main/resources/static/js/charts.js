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
                console.log(selection);
            }
        }
    }
});






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
              innerHtml += "</ul>";
              innerHtml += "<div class=\"description-text-div\">" + out.description.replaceAll(/\. ([A-Z])/gm, "\.<br\/>$1") + "</div>";

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

function createAd() {
  adHtml = `
      <ins class="adsbygoogle"
           style="display:block"
           data-ad-client="ca-pub-4680762769337689"
           data-ad-slot="8763896973"
           data-ad-format="auto"
//           data-adtest="on"
           data-full-width-responsive="true"></ins>
      <script>
           (adsbygoogle = window.adsbygoogle || []).push({});
      </script>`;

  //$("#charts").append(adHtml); // waiting on getting approved
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

createAd();

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
createChart("/financials/cape_ratio", "CAPE ratio", {suggestedMin: -5, suggestedMax: 100, quarterlyEnabled: false});
createChart("/financials/past_pe_to_growth_ratio", "Trailing PEG ratio", {
  tooltip: 'Trailing PEG is the PE ratio divided by the median past [1..7] year annual EPS growth. Generally above 2 is expensive, below 1 is cheap.',
  suggestedMin: -2,
  suggestedMax: 5,
  quarterlyEnabled: false});


createChart("/financials/past_cape_to_growth_ratio", "Trailing PEG ratio variations", {
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
]});
createChart("/financials/ev_over_ebitda", "EV over ebitda", {});

createChart("/financials/p2b_ratio", "Price to Book ratio", {
  suggestedMin: 0,
  suggestedMax: 15,
  quarterlyEnabled: false,
  label: "price / book value"
});
createChart("/financials/quick_ratio", "Quick ratio", {suggestedMin: 0, suggestedMax: 5, quarterlyEnabled: false});

createAd();

createSeparator("Debt information")

createChart("/financials/cash_flow_to_debt", "Operating cash flow debt coverage", {unit: '%', quarterlyEnabled: false});
createChart("/financials/short_term_coverage_ratio", "Operating cash flow short term debt coverage", {unit: '%', quarterlyEnabled: false});
createChart("/financials/short_term_assets_to_total_debt", "Short term assets to total debt", {quarterlyEnabled: false});
createChart("/financials/altmanz", "Altman Z score", {quarterlyEnabled: false});
createChart("/financials/interest_expense", "Interest expense", {});
createChart("/financials/interest_rate", "Interest rate", {unit: '%', quarterlyEnabled: false});
createChart("/financials/interest_coverage", "EBIT / interest", {unit: 'x'});



createSeparator("Assets")
createChart("/financials/current_assets", "Current assets vs liabilities", {label: "Current assets", quarterlyEnabled: false, additionalCharts: [
  {
    "url": "/financials/current_liabilities",
    "label": "Current liabilities"
  }
]});
createChart("/financials/total_assets", "Total assets vs liabilities", {label: "Total assets", quarterlyEnabled: false, additionalCharts: [
  {
    "url": "/financials/total_liabilities",
    "label": "Total liabilities"
  }
]});
createChart("/financials/intangible_assets_percent", "Intangible assets", {
   unit: '%',
   quarterlyEnabled: false,
   label: "Total intangible assets to total assets",
   additionalCharts: [
   {
     "url": "/financials/goodwill_percent",
     "label": "Goodwill to total assets"
   }
]});
createChart("/financials/cash", "Cash and cash equivalents", {quarterlyEnabled: false});

//createChart("/financials/non_current_assets", "Non current assets", {quarterlyEnabled: false});
//createChart("/financials/long_term_debt", "Long term debt", {quarterlyEnabled: false});

createAd();

createSeparator("Return ratios")
createChart("/financials/roic", "Return on invested capital", {unit: '%', quarterlyEnabled: false});
createChart("/financials/return_on_assets", "Return on assets", {unit: '%', quarterlyEnabled: false});
createChart("/financials/return_on_tangible_assets", "Return on tangible assets", {unit: '%', quarterlyEnabled: false});


createSeparator("Yields")
createChart("/financials/eps_yield", "Earnings flow yield", {suggestedMin: -2, suggestedMax: 50, unit: '%', label: "EPS yield", quarterlyEnabled: false,
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
//createChart("/financials/expected_return", "Compsite yield", {unit: '%'});


createSeparator("Other")
createChart("/financials/market_cap_usd", "Market cap $", {quarterlyEnabled: false, label: '$'});
createChart("/financials/share_count", "Share count", {});
createChart("/financials/stock_compensation", "Stock compensation", {quarterlyEnabled: false});
createChart("/financials/stock_compensation_per_net_income", "Stock compensation / net income", {suggestedMin: -2, unit: '%'});
createChart("/financials/stock_compensation_per_net_revenue", "Stock compensation / revenue", {suggestedMin: 0, unit: '%'});
createChart("/financials/stock_compensation_per_market_cap", "Stock compensation / market cap", {suggestedMin: 0, unit: '%'});
createChart("/financials/capex_to_revenue", "CAPEX to revenue", {unit: '%'});
createChart("/financials/acquisitions_per_market_cap", "Acquisitions to marketcap", {unit: '%'});
createChart("/financials/pietrosky_score", "Pietrosky score", {quarterlyEnabled: false});



createSeparator("Dividend")
createChart("/financials/dividend_yield", "Dividend yield", {unit: '%', suggestedMin: -2, suggestedMax: 50, quarterlyEnabled: false});
createChart("/financials/dividend_payout_ratio", "Dividend payout ratio", {unit: '%', suggestedMin: -2, suggestedMax: 150});
createChart("/financials/dividend_payout_ratio_with_fcf", "Dividend payout ratio FCF", {unit: '%', suggestedMin: -2, suggestedMax: 150});
createChart("/financials/dividend_paid", "Dividend paid", {quarterlyEnabled: false});
createChart("/financials/dividend_yield_per_current_price", "Dividend yield for invest price", {unit: '%', suggestedMin: -2, suggestedMax: 200});

createAd();


createSeparator("Growth")
createChart("/financials/eps_growth_rate", "EPS growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChart("/financials/eps_growth_rate_7yr_moving_avg", "EPS annual growth x year intervals", {
  type: 'bar',
  unit: '%',
  quarterlyEnabled: false,
  tooltip: 'Shows the annual EPS growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized EPS growth between 2013 and 2020.',
  slider: {
    id: "eps_growth_year_slider",
    parameterName: "year",
    min: 2,
    max: 10,
    default: 7
}});

createChart("/financials/fcf_growth_rate", "FCF growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChart("/financials/fcf_growth_rate_7yr_moving_avg", "FCF annual growth x year intervals", {
  type: 'bar',
  unit: '%',
  quarterlyEnabled: false,
  tooltip: 'Shows the annual FCF/share growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized FCF/share growth between 2013 and 2020.',
  slider: {
    id: "fcf_growth_year_slider",
    parameterName: "year",
    min: 2,
    max: 10,
    default: 7
}});

createChart("/financials/revenue_growth_rate", "Revenue growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false,});

createChart("/financials/revenue_growth_rate_xyr_moving_avg", "Revenue annual growth x year intervals", {
      type: 'bar',
      unit: '%',
      lazyLoad: false,
      quarterlyEnabled: false,
      tooltip: 'Shows the annual revenue growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized revenue growth between 2013 and 2020.',
      slider: {
        id: "revenue_growth_rate_xyr_moving_avg_slider",
        parameterName: "year",
        min: 2,
        max: 10,
        default: 7
}});
createChart("/financials/share_count_growth_rate", "Share count growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChart("/financials/dividend_growth_rate", "Dividend growth annual", {type: 'bar', unit: '%', quarterlyEnabled: false});

createAd();

createSeparator("DCF")
createChart("/financials/eps_dcf", "EPS DCF", {quarterlyEnabled: false});
createChart("/financials/fcf_dcf", "FCF DCF", {quarterlyEnabled: false});
createChart("/financials/dividend_dcf", "Dividend DCF", {quarterlyEnabled: false});
createChart("/financials/cash_per_share", "Cash per share", {quarterlyEnabled: false});
createChart("/financials/graham_number", "Graham number", {quarterlyEnabled: false});

createAd();

createSeparator("Price")
createChart("/financials/price", "Price vs calculated fair value", {
   label: "price",
   quarterlyEnabled: false,
   tooltip: 'Calculated fair value is the value you get with the default form on the calculator page.',
   additionalCharts: [
  {
    "url": "/financials/default_calculator_result",
    "label": "Calculated fair value"
  }
]});
createChart("/financials/return_with_reinvested_dividend", "Total returns", {
  label: "Returns with reinvested dividends",
  quarterlyEnabled: false,
  tooltip: 'Returns if each dividend received is reinvested into more stocks (assumes no transaction fee or tax)',
  additionalCharts: [{
    "url": "/financials/price",
    "label": "price"
  }
]});
createChart("/financials/price_growth_rate", "Price growth", {type: 'bar', quarterlyEnabled: false});

createAd();

addFlags();

var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
  console.log("ASDASDASD " + tooltipTriggerEl);
  return new bootstrap.Tooltip(tooltipTriggerEl)
})