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
    console.log(date);
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

  function updateCalculation() {
  console.log("Update calc");
      revenue = Number($("#revenue").val()) * 1000000;
      startGrowth = Number($("#startGrowth").val()) / 100.0 + 1.0;
      endGrowth = Number($("#endGrowth").val()) / 100.0 + 1.0;
      startMargin = Number($("#startMargin").val()) / 100.0;
      endMargin = Number($("#endMargin").val()) / 100.0;
      shareChange = Number($("#shareChange").val()) / 100.0 + 1.0;
      discount = Number($("#discount").val()) / 100.0;
      shareCount = Number($("#shareCount").val()) * 1000;
      endMultiple = Number($("#endMultiple").val());
      currentPrice = Number($("#current-price").text());
      
      years = 10;
      
      epses = [];
      revenues = [];
      margins = [];
      

      
      if (!isNaN(revenue) && !isNaN(startGrowth) && !isNaN(endGrowth) && !isNaN(startMargin) && !isNaN(endMargin) && !isNaN(shareChange) && !isNaN(discount) && !isNaN(endMultiple)) {
         value = 0.0;
         previousRevenue = revenue;
         previousShareCount = shareCount;
         epsSum = 0.0;
         for (i = 0; i < years; ++i) {
            currentGrowth = startGrowth - ((startGrowth - endGrowth) * i) / (years - 1);
            currentMargin = startMargin - ((startMargin - endMargin) * i) / (years - 1);

            previousRevenue = previousRevenue * currentGrowth;
            previousShareCount = previousShareCount * shareChange;
                        
            
            netIncome = previousRevenue * currentMargin;
            
            eps = netIncome / previousShareCount;
            
            epses.push({x: dates[i], y: eps});
            revenues.push({x: dates[i], y: previousRevenue});
            margins.push({x: dates[i], y: currentMargin * 100.0});
            epsSum += eps;
            
            console.log(1.0 + discount);
            
            discountedEps = (eps / Math.pow(1.0 + discount, i + 1));
            console.log(discountedEps);
            value += discountedEps;
            
            
            
            
            $(revInputs[i]).val((previousRevenue / 1000000).toFixed(2));
            $(epsInputs[i]).val(eps.toFixed(2));
            $(dEpsInputs[i]).val(discountedEps.toFixed(2));
         }
         
         value += ((eps * endMultiple) / Math.pow(1.0 + discount, years));
         epsSum += eps * endMultiple;
         
         marginOfSafety = (value / currentPrice - 1.0) * 100.0;
         endPrice = (eps * endMultiple);
         expectedGrowth = Math.pow(epsSum / currentPrice, (1.0 / years)) - 1.0;
         
         $("#fair_value").text("Value: " + value.toFixed(2));
         $("#current_price").html("Current price: " + currentPrice.toFixed(2) + " (Margin of safety: <b>" + marginOfSafety.toFixed(2) + "%</b>, "
               + "price in ten years: <b>" + endPrice.toFixed(2) + "</b>)");
      }
      

      updateChart(chart, epses);
      updateChart(revChart, revenues);
      updateChart(marginChart, margins);
  }

  $('#dcf_form input').each(
      function(index) {  
          var input = $(this);
          input.on("input", function() {
            updateCalculation();
          })
      });
   
   
  var stock = document.getElementById("stock").innerText;
  var stockToLoad = document.getElementById("stock").innerText;
  chart=createChart("/financials/eps", "EPS", {
       additionalLabelsAtEnd: dates,
       label: "past",
       runAfter: updateCalculation
  });
  
   chart.options.scales.x = {
          display: true,
                type: 'time',
                time: {
                    displayFormats: {
                        quarter: 'YYYY-MM-DD'
                    }
                }
        }
    chart.update();
    
    
    revChart=createChart("/financials/revenue", "revenue", {
         additionalLabelsAtEnd: dates,
         label: "past",
         runAfter: updateCalculation
    });
    
     revChart.options.scales.x = {
            display: true,
                type: 'time',
                time: {
                    displayFormats: {
                        quarter: 'YYYY-MM-DD'
                    }
                }
        }
    revChart.update();
    
    
    
    
    marginChart=createChart("/financials/net_margin", "net margin", {
         additionalLabelsAtEnd: dates,
         label: "past",
         runAfter: updateCalculation
    });
    
     marginChart.options.scales.x = {
            display: true,
                type: 'time',
                time: {
                    displayFormats: {
                        quarter: 'YYYY-MM-DD'
                    }
                }
        }
    marginChart.update();
    
    createChart("/financials/revenue_growth_rate_xyr_moving_avg", "Revenue annual growth x year intervals", {
      type: 'bar',
      unit: '%',
      slider: {
        id: "revenue_growth_rate_xyr_moving_avg_slider",
        parameterName: "year",
        min: 2,
        max: 10,
        default: 7
    }});