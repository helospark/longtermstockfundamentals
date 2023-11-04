
  function pad(num, size) {
      num = num.toString();
      while (num.length < size) num = "0" + num;
      return num;
  }
  
  function dateToString(date) {
    result = date.getFullYear();
    result += "-" + pad(date.getMonth() + 1, 2);
    result += "-" + pad(date.getDay() + 1, 2);
    return result;
  }
  
  dcfTable=$("#calculation_result");
  
  revInputs = [];
  epsInputs = [];
  dEpsInputs = [];
  dates = [];
  
  dateObj = new Date();
  dateObj.setFullYear(dateObj.getFullYear() + 1);
  
  for (i=0; i < 10; ++i) {
    liDateElement = document.createElement("td");
    date = dateToString(dateObj);
    liDateElement.innerText = dateObj.getFullYear();
    dates.push(date);
    $("#table_dates").append(liDateElement);
    
    depsInput = document.createElement("input");
    depsInput.className="yearInput";
    dEpsInputs.push(depsInput);
    liEpsElement = document.createElement("td");
    liEpsElement.appendChild(depsInput);
    $("#table_deps").append(liEpsElement);
    
    
    epsInput = document.createElement("input");
    epsInput.className="yearInput";
    epsInputs.push(epsInput);
    liEpsElement = document.createElement("td");
    liEpsElement.appendChild(epsInput);
    $("#table_eps").append(liEpsElement);
    
    revInput = document.createElement("input");
    revInput.className="yearInput";
    revInputs.push(revInput);
    liEpsElement = document.createElement("td");
    liEpsElement.appendChild(revInput);
    $("#table_revenue").append(liEpsElement);
    
    
    
    dateObj.setFullYear(dateObj.getFullYear() + 1);
  }


  function updateChart(chart, epses) {
      currentLabels = chart.data.labels;
      currentData = chart.data.datasets[0].data;
      
      if (currentLabels.length > years && currentData.length > 0) {
        epses.unshift({x: currentLabels[currentLabels.length - 1 - years], y: currentData[currentData.length - 1]});
      }
  
      if (chart.data.datasets.length < 2) {
       chart.data.datasets[1] = {
          pointRadius: 2,
          data: epses,
          pointHitRadius: 300,
          label: "projection",
            borderColor: "red",
            backgroundColor: "red"
        };
      } else {
        for (i = 0; i < epses.length; ++i) {
          chart.data.datasets[1].data[i] = epses[i];
        }
      }
      
      maxEps = 0.0;
      for (i = 0; i < epses.length; ++i) {
        if (i == 0 || maxEps < epses[i].y) {
          maxEps = epses[i].y;
        }
      }
      
      currentMax = Math.max.apply(Math, currentData);

      if (currentMax > maxEps && !isNaN(currentMax)) {
        maxEps = currentMax;
      }
      maxEps *= 1.05;
      if (!isNaN(maxEps)) {
        chart.options.scales.y.max = maxEps;
      }
      chart.update();
  }
  
  function forwardDcf(request) {
      var revenue = request.revenue;
      var startGrowth = request.startGrowth;
      var endGrowth = request.endGrowth;
      var startMargin = request.startMargin;
      var endMargin = request.endMargin;
      var startShareChange = request.startShareChange;
      var endShareChange = request.endShareChange;
      var discount = request.discount;
      var shareCount = request.shareCount;
      var endMultiple = request.endMultiple;
      var currentPrice = request.currentPrice;
      var startPayoutRatio = request.startPayoutRatio;
      var endPayoutRatio = request.endPayoutRatio;
      
      console.log("Payout ratio= " + startPayoutRatio);
      
      var years = request.years;
  
      var epses = [];
      var revenues = [];
      var margins = [];
      var shareCounts = [];
      var totalPayouts = [];

      if (!isNaN(revenue) && !isNaN(startGrowth) && !isNaN(endGrowth) && !isNaN(startMargin) && !isNaN(endMargin) && !isNaN(startShareChange) && !isNaN(discount) && !isNaN(endMultiple)) {
         var value = 0.0;
         var previousRevenue = revenue;
         var previousShareCount = shareCount;
         var epsSum = 0.0;
         for (i = 0; i < years; ++i) {
            currentGrowth = startGrowth - ((startGrowth - endGrowth) * i) / (years - 1);
            currentMargin = startMargin - ((startMargin - endMargin) * i) / (years - 1);
            currentShareChange = startShareChange - ((startShareChange - endShareChange) * i) / (years - 1);
            payoutRatio = startPayoutRatio - ((startPayoutRatio - endPayoutRatio) * i) / (years - 1);

            previousRevenue = previousRevenue * currentGrowth;
            previousShareCount = previousShareCount * currentShareChange;
                        
            
            netIncome = previousRevenue * currentMargin;
            
            eps = (netIncome / shareCount) * payoutRatio;
            unalteredEps = (netIncome / previousShareCount);
            
            epses.push({x: dates[i], y: unalteredEps});
            revenues.push({x: dates[i], y: previousRevenue});
            margins.push({x: dates[i], y: currentMargin * 100.0});
            shareCounts.push({x: dates[i], y: previousShareCount});
            totalPayouts.push({x: dates[i], y: payoutRatio * 100.0});
            epsSum += (eps);
            
            discountedEps = (eps / Math.pow(1.0 + discount, i + 1));
            value += discountedEps;
            
            
            
            
            $(revInputs[i]).val((previousRevenue / 1000000).toFixed(2));
            $(epsInputs[i]).val(eps.toFixed(2));
            $(dEpsInputs[i]).val(discountedEps.toFixed(2));
         }
         }
         
         eps = (netIncome / previousShareCount);
         value += ((eps * endMultiple) / Math.pow(1.0 + discount, years));
         epsSum += eps * endMultiple;
         
         var marginOfSafety = (value / currentPrice - 1.0) * 100.0;
         var endPrice = (eps * endMultiple);
         var expectedGrowth = Math.pow(epsSum / currentPrice, (1.0 / years)) - 1.0;
         
         return [value, marginOfSafety, endPrice, expectedGrowth, epses, revenues, margins, shareCounts, totalPayouts];
  }
  
  function reverseDcf(request, originalMarginOfSafety) {
    lowerBound = -0.999;
    upperBound = 1;
    if (originalMarginOfSafety < 0) {
      upperBound = request.discount;
    } else if (originalMarginOfSafety > 0) {
      lowerBound = request.discount;
    }
    

    var i = 0;
    while (lowerBound < upperBound && i < 10) {
      currentBound = (upperBound + lowerBound) / 2.0;
      newRequest = {...request, discount: currentBound};
      const [value, marginOfSafety] = forwardDcf(newRequest);
      
      if (marginOfSafety < 0) {
        upperBound = currentBound;
      } else if (marginOfSafety > 0) {
        lowerBound = currentBound;
      }
      if (Math.abs(marginOfSafety) < 0.5) {
        break;
      }
      ++i;
    }

    return currentBound;
  }

  function updateCalculation() {
      request = {};
      request.revenue = Number($("#revenue").val()) * 1000000;
      request.startGrowth = Number($("#startGrowth").val()) / 100.0 + 1.0;
      request.endGrowth = Number($("#endGrowth").val()) / 100.0 + 1.0;
      request.startMargin = Number($("#startMargin").val()) / 100.0;
      request.endMargin = Number($("#endMargin").val()) / 100.0;
      request.startShareChange = Number($("#shareChange").val()) / 100.0 + 1.0;
      request.endShareChange = Number($("#endShareChange").val()) / 100.0 + 1.0;
      request.discount = Number($("#discount").val()) / 100.0;
      request.shareCount = Number($("#shareCount").val()) * 1000;
      request.endMultiple = Number($("#endMultiple").val());
      request.startPayoutRatio = Number($("#startPayout").val()) / 100.0;
      request.endPayoutRatio = Number($("#endPayout").val()) / 100.0;
      request.currentPrice = Number($("#current-price").text());
      currentPriceInTradingCurrency = Number($("#current-price-in-trading-currency").text());
      exchangeRate = Number($("#reporting-currency-to-trading-currency-exchange-rate").text());
      currencySymbol = $("#trading-currency-symbol").text();
      
      years = 10;
      request.years = years;
      
      
      [value, marginOfSafety, endPrice, expectedGrowth, epses, revenues, margins, shareCounts, totalPayouts] =  forwardDcf(request);
      
      
      expectedGrowth = reverseDcf(request, marginOfSafety);
      
         $("#fair_value").html("Value: " + currencySymbol + "<span id=\"fair-value\">" + convertFx(value, exchangeRate).toFixed(2) + "</span>");
         $("#current_price").html("Current price: " + currencySymbol + currentPriceInTradingCurrency.toFixed(2) + " (Margin of safety: <b>" + marginOfSafety.toFixed(2) + "%</b>, "
               + "price in ten years: <b>" + currencySymbol + convertFx(endPrice, exchangeRate).toFixed(2) + "</b>, expected return: <b>" + (expectedGrowth*100.0).toFixed(2) + "%</b>)");
            
      if (chart !== undefined) {
        updateChart(chart, epses);
      }
      if (revChart !== undefined) {
        updateChart(revChart, revenues);
      }
      if (marginChart !== undefined) {
        updateChart(marginChart, margins);
      }
      if (shareCountChart !== undefined) {
        updateChart(shareCountChart, shareCounts);
      }
      if (totalPayouts !== undefined) {
        updateChart(totalPayoutChart, totalPayouts);
      }
  }
  
  function convertFx(a, fx) {
    return a*fx;
  }

  $('#dcf_form input').each(
      function(index) {  
          var input = $(this);
          input.on("input", function() {
            updateCalculation();
          })
      });
   
   
  var stock = document.getElementById("stock").innerText;
  var stockToLoad = document.getElementById("stockToLoad").innerText;
  var calculatorType = document.getElementById("calculatorType").innerText;

  if (calculatorType === 'eps') {
    chart=createChart("/financials/eps", "EPS", {
         additionalLabelsAtEnd: dates,
         lazyLoading: false,
         label: "past",
         quarterlyEnabled: false,
         runAfter: updateCalculation,
         zeroBasedChangeListener: updateCalculation
    });
  } else if (calculatorType === 'fcf') {
    chart=createChart("/financials/pfcf", "FCF / share", {
         additionalLabelsAtEnd: dates,
         lazyLoading: false,
         label: "past",
         quarterlyEnabled: false,
         runAfter: updateCalculation,
         zeroBasedChangeListener: updateCalculation
    });
  }

    revChart=createChart("/financials/revenue", "Revenue", {
         additionalLabelsAtEnd: dates,
         lazyLoading: false,
         label: "past",
         quarterlyEnabled: false,
         runAfter: updateCalculation,
         zeroBasedChangeListener: updateCalculation
    });
    
    if (calculatorType === 'eps') {
      marginChart=createChart("/financials/net_margin", "net margin", {
           additionalLabelsAtEnd: dates,
           label: "past",
           runAfter: updateCalculation,
           zeroBasedChangeListener: updateCalculation,
           quarterlyEnabled: false,
           lazyLoading: false
      });
    } else if (calculatorType === 'fcf') {
      marginChart=createChart("/financials/fcf_margin", "FCF margin", {
           additionalLabelsAtEnd: dates,
           label: "past",
           runAfter: updateCalculation,
           zeroBasedChangeListener: updateCalculation,
           quarterlyEnabled: false,
           lazyLoading: false
      });
    }
    
    shareCountChart = createChart("/financials/share_count", "Share count", {
         additionalLabelsAtEnd: dates,
         label: "past",
         runAfter: updateCalculation,
         zeroBasedChangeListener: updateCalculation,
         quarterlyEnabled: false,
         lazyLoad: false
     });

    if (calculatorType === 'eps') {
      totalPayoutChart = createChart("/financials/total_payout_ratio", "Total payout ratio", {
           additionalLabelsAtEnd: dates,
           label: "past",
           runAfter: updateCalculation,
           zeroBasedChangeListener: updateCalculation,
           quarterlyEnabled: false,
           lazyLoad: false
       });
     } else if (calculatorType === 'fcf') {
      totalPayoutChart = createChart("/financials/total_payout_ratio_fcf", "Total payout ratio", {
           additionalLabelsAtEnd: dates,
           label: "past",
           runAfter: updateCalculation,
           zeroBasedChangeListener: updateCalculation,
           quarterlyEnabled: false,
           lazyLoad: false
       });
     }
     
    
    createChart("/financials/revenue_growth_rate_xyr_moving_avg", "Revenue annual growth x year intervals", {
      type: 'bar',
      unit: '%',
      lazyLoading: false,
      quarterlyEnabled: false,
      slider: {
        id: "revenue_growth_rate_xyr_moving_avg_slider",
        parameterName: "year",
        min: 1,
        max: 10,
        default: 7
    }});
    
    updateCalculation();