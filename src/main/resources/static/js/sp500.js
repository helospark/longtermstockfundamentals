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






var stock = "sp500";
var stockToLoad = "sp500";

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

createSeparator("Price");
createChart("/data/price", "Price", {
  label: "Price",
  unit: '$',
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/price_infl_adjusted",
    "label": "Inflation adjusted price"
  }]
});
createChart("/data/price_with_reinvested_dividends", "Price with reinvested dividend", {
  label: "Price",
  unit: '$',
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/price_reinv_dividends_infl_adjusted",
    "label": "Price with reinvested dividends (infl adjusted)"
  }]
});
createChart("/data/price_with_reinvested_dividends", "Price with reinvested dividend", {
  label: "Price",
  unit: '$',
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/price",
    "label": "Price with reinvested dividends (infl adjusted)"
  }]
});


createSeparator("Growth");
createChart("/data/price_growth", "Annual growth till today", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChart("/data/price_growth_intervals", "Annual growth x year intervals", {
      type: 'bar',
      unit: '%',
      quarterlyEnabled: false,
      tooltip: 'Shows the annual growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized growth between 2013 and 2020.',
      slider: {
        id: "price_growth_intervals",
        parameterName: "year",
        min: 1,
        max: 70,
        default: 10
}});

createChart("/data/price_growth_reinv_dividends", "Annual growth till today with reinvested dividends", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChart("/data/price_growth_reinv_dividends_x_yr", "Dividends reinvested growth x year intervals", {
      type: 'bar',
      unit: '%',
      quarterlyEnabled: false,
      tooltip: 'Shows the annual growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized growth between 2013 and 2020.',
      slider: {
        id: "price_growth_intervals_divs",
        parameterName: "year",
        min: 1,
        max: 70,
        default: 10
}});

createChart("/data/price_growth_reinv_dividends_infl_adjust", "Annual growth till today with reinvested dividends (inflation adjusted)", {type: 'bar', unit: '%', quarterlyEnabled: false});
createChart("/data/price_growth_x_yrs_intervals_divs_infl_adjusted", "Dividends reinvested, inflation adjusted growth x year intervals", {
      type: 'bar',
      unit: '%',
      quarterlyEnabled: false,
      tooltip: 'Shows the annual growth every x year intervals, where x is selected with the slider.<br/>So for example when 7 year is selected then at 2020 it shows the annualized growth between 2013 and 2020.',
      slider: {
        id: "price_growth_intervals_divs_infl",
        parameterName: "year",
        min: 1,
        max: 70,
        default: 10
}});

/*
createSeparator("Price vs economy");
createChart("/data/price_for_indicator", "Price vs unemployment", {
  label: "Price",
  unit: '%',
  staticParameters: {
    "indicator": "unemploymentRate"
  },
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/indicator",
    "label": "Unemployment rate",
    "secondYAxis": true
  }]
});
createChart("/data/price_for_indicator", "Price vs consumer sentiment", {
  label: "Price",
  staticParameters: {
    "indicator": "consumerSentiment"
  },
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/indicator",
    "label": "Consumer sentiment",
    "secondYAxis": true
  }]
});
createChart("/data/price_for_indicator", "Price vs real GDP", {
  label: "Price",
  unit: '$',
  staticParameters: {
    "indicator": "realGDP"
  },
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/indicator",
    "label": "Real GDP",
    "secondYAxis": true
  }]
});
createChart("/data/price_for_indicator", "Price vs GDP", {
  label: "Price",
  unit: '$',
  staticParameters: {
    "indicator": "GDP"
  },
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/indicator",
    "label": "GDP",
    "secondYAxis": true
  }]
});
createChart("/data/price_for_indicator", "Price vs mortgage", {
  label: "Price",
  unit: '$',
  staticParameters: {
    "indicator": "15YearFixedRateMortgageAverage"
  },
  quarterlyEnabled: false,
  additionalCharts: [{
    "url": "/data/indicator",
    "label": "15 year fixed mortgage",
    "secondYAxis": true
  }]
});
*/


createBubbleChart("/sp500/data/xyr_shiller_return", "Shiller vs x year returns (inflation adjusted including dividends)", {
  addStockPrefix: false,
  slider: {
    id: "asd1",
    parameterName: "year",
    min: 1,
    max: 40,
    default: 10
  }
});
createBubbleChart("/sp500/data/xyr_pe_return", "PE vs x year returns (inflation adjusted including dividends)", {
  addStockPrefix: false,
  slider: {
      id: "asd2",
      parameterName: "year",
      min: 1,
      max: 40,
      default: 10
  }
});
createBubbleChart("/sp500/data/spgdpratio_return", "S&P500 to GDP ratio vs x year returns (inflation adjusted including dividends)", {
  addStockPrefix: false,
  slider: {
      id: "asd3",
      parameterName: "year",
      min: 1,
      max: 40,
      default: 10
  }
});
createBubbleChart("/sp500/data/buffet_indicator_return", "Buffet indicator vs x year returns (inflation adjusted including dividends)", {
  addStockPrefix: false,
  slider: {
      id: "asd3",
      parameterName: "year",
      min: 1,
      max: 40,
      default: 10
  }
});
createBubbleChart("/sp500/data/interestrate_return", "Fed funds rate vs x year returns (inflation adjusted including dividends)", {
  addStockPrefix: false,
  slider: {
      id: "asd4",
      parameterName: "year",
      min: 1,
      max: 40,
      default: 10
  }
});
createBubbleChart("/sp500/data/inflatation_return", "Inflation rate vs x year returns (inflation adjusted including dividends)", {
  addStockPrefix: false,
  slider: {
      id: "asd5",
      parameterName: "year",
      min: 1,
      max: 40,
      default: 10
  }
});


var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
  return new bootstrap.Tooltip(tooltipTriggerEl)
})