var constColorPalette = [
  "rgba(0,0,255,0.3)",
  "rgba(255,0,0,0.3)",
  "rgba(75,200,32,0.3)",
  "rgba(200,200,0,0.8)",
  "rgba(0,200,200,0.8)",
  "rgba(100,100,100,0.8)",
];
var constColorPaletteLine = [
  "rgba(0,0,255,0.8)",
  "rgba(255,0,0,0.8)",
  "rgba(75,200,32,0.8)",
  "rgba(200,200,0,0.8)",
  "rgba(0,200,200,0.8)",
  "rgba(100,100,100,0.8)",
];

function createCheckbox(label) {
  var checkbox = document.createElement("input");
  checkbox.type="checkbox";
  checkbox.innerHtml=label;
  

  return label;
}

const plugin = {
    id: 'corsair',
    lastElement: {},
    downElement: {},
    cagr: {},
    defaults: {
        width: 1,
        color: '#FF4949',
        dash: [3, 3],
        continousTooltipCagr: false
    },
    afterEvent: (chart, args, opts) => {
      const {inChartArea} = args
      const {type,x,y} = args.event
      
      var isSameChart = (chart.canvas === plugin.lastElement.canvas);
      
     if (plugin.downElement.dataset !== plugin.lastElement.dataset && !opts.continousTooltipCagr) {
       plugin.cagr = {};
     }

     if (args.event.type === "mousedown") {
        plugin.downElement = plugin.lastElement;
        plugin.downElement.x = x;
     }
     if (args.event.type === "mousemove" &&
             plugin.downElement.label !== undefined &&
             plugin.lastElement.label !== undefined &&
             plugin.downElement.label != plugin.lastElement.label &&
             (plugin.downElement.dataset === plugin.lastElement.dataset || opts.continousTooltipCagr)) {
       
        plugin.lastElement.x = x;
       
       var dateObj1 = new Date(plugin.downElement.label);
       var dateObj2 = new Date(plugin.lastElement.label);

       var then = plugin.downElement.data;
       var now = plugin.lastElement.data;

       if (dateObj1 > dateObj2) {
         var tmp = dateObj2;
         dateObj2 = dateObj1;
         dateObj1 = tmp;
         
         tmp = then;
         then = now;
         now = tmp;
       }
       
       if (typeof now === 'object') {
         now = now.y;
       }

      if (!(isNaN(dateObj1.getTime()) || isNaN(dateObj2.getTime()))) {
            const timeDiff = Math.abs(dateObj2.getTime() - dateObj1.getTime());
            const yearsDiff = timeDiff / (1000 * 60 * 60 * 24 * 365.0);


            var cagrInternal = NaN;

            if (now < 0.0 && then < 0.0) {
                cagrInternal =  -(Math.pow(now / then, 1.0 / yearsDiff) - 1.0) * 100.0;
            } else if (now > 0.0 && then > 0.0) {
                cagrInternal =  (Math.pow(now / then, 1.0 / yearsDiff) - 1.0) * 100.0;
            }

            var totalChange = (now / then - 1.0) * 100.0;
            
            if (then < 0.0 && then < 0) {
              totalChange *= -1.0;
            }
            
            if (Number.isFinite(totalChange) && Number.isFinite(cagrInternal)) {
              plugin.cagr = {
                init: true,
                startDate: dateObj1.toISOString().slice(0, 10),
                endDate: dateObj2.toISOString().slice(0, 10),
                change: totalChange,
                cagr: cagrInternal
              };
            } else {
              plugin.cagr = {};
            }
        }
     }
     if (args.event.type === "mouseup" || args.event.type === "click") {
       plugin.downElement = {};
       plugin.cagr = {};
     }

     //chart.draw()
    },
    beforeDatasetsDraw: (chart, args, opts) => {
      const {ctx} = chart
      const {top, bottom, left, right} = chart.chartArea

      if (plugin.downElement.x !== undefined && plugin.downElement.canvas === chart.canvas) {
        ctx.save()
        
        ctx.beginPath()
        ctx.lineWidth = opts.width
        ctx.strokeStyle = opts.color
        ctx.setLineDash(opts.dash)
        
        ctx.moveTo(plugin.downElement.x, bottom)
        ctx.lineTo(plugin.downElement.x, top)
        
        ctx.moveTo(plugin.lastElement.x, bottom)
        ctx.lineTo(plugin.lastElement.x, top)

        ctx.stroke()
      
        ctx.restore()
      }
    }
  }



function createChart(urlPath, title, chartOptions) {
  var canvas =document.createElement("canvas");
  canvas.style='width:100%;max-height:400px'
  
  var underChartBar = document.createElement("div");
  underChartBar.style="width:100%";
  
  var chartDiv = document.createElement("div");
  chartDiv.className="chartDiv";
  
  var titleDiv = document.createElement("h2");
  titleDiv.innerText = title;
  titleDiv.className="chart-title";
  
  if (chartOptions.tooltip !== undefined) {
    toolTip = $("<a class=\"chart-tooltip-link\" data-bs-html=\"true\" title=\"" + chartOptions.tooltip + "\" data-bs-toggle=\"tooltip\"><i class=\"far fa-question-circle\"></i></a>").get(0);
    titleDiv.appendChild(toolTip);
  }
  
  dropDown = $("#chart-dropdown");
  if (dropDown.length > 0) {
    titleName=title.toLowerCase().replace(/[^a-z0-9]+/gi, "_");
    var titleLink = $("<a name=\"" + titleName + "\"></a>");
    titleLink.append(titleDiv);
    titleDiv = titleLink.get(0);
    dropDown.append("<li><a class=\"dropdown-item\" href=\"#" + titleName + "\">" + title + "</a></li>");
  }
  
  
  var element = document.getElementById("charts");
  element.appendChild(titleDiv);
  element.appendChild(chartDiv);
  element.appendChild(underChartBar);

  var xValues = [];
  var yValues = [];
  type = chartOptions.type !== undefined ? chartOptions.type : 'line';
  suggestedMin = chartOptions.suggestedMin !== undefined ? chartOptions.suggestedMin : undefined;
  var unit = chartOptions.unit !== undefined ? " " + chartOptions.unit : "";
  var label = chartOptions.label === undefined ? "data" : chartOptions.label;
  var legendDisplay = chartOptions.additionalCharts === undefined ? false : true;
  var animation = chartOptions.animation === undefined ? false : chartOptions.animation;
  var isLazyLoading = chartOptions.lazyLoading === undefined ? true : chartOptions.lazyLoading;
  var dated = chartOptions.dated === undefined ? true : chartOptions.dated;
  var defaultQuarterlyEnabled = chartOptions.defaultQuarterlyEnabled === undefined ? false : chartOptions.defaultQuarterlyEnabled;
  var quaterlySupported = chartOptions.quarterlyEnabled === undefined ? true : chartOptions.quarterlyEnabled;
  var isSecondYAxisNeeded = chartOptions.additionalCharts !== undefined && chartOptions.additionalCharts[0].secondYAxis === true ? true : false;
  var addStockPrefix = chartOptions.addStockPrefix !== undefined ? chartOptions.addStockPrefix : true;
  var continousTooltipCagr = chartOptions.continousTooltipCagr !== undefined ? chartOptions.continousTooltipCagr : false;
  var stacked = chartOptions.stacked !== undefined ? chartOptions.stacked : false;
  
  if (!addStockPrefix) {
    stockToLoad = "";
  }
  
  var colorPaletteLine = constColorPaletteLine;
  var colorPalette = constColorPalette;
  
  if (type == "bar") {
    colorPalette = ["rgba(0,0,255,0.6)","rgba(255,0,0,0.6)",];
  }
  
  var additionalLabelsAtEnd = chartOptions.additionalLabelsAtEnd === undefined ? [] : chartOptions.additionalLabelsAtEnd;
  
  var chartConfig = {
    type: type,
    data: {
      labels: xValues,
      datasets: [{
        fill: true,
        pointRadius: 2,
        borderColor: colorPaletteLine[0],
        backgroundColor: colorPalette[0],
        data: yValues,
        pointHitRadius: 300,
        label: label,
        yAxisID: "y"
      }]
    },
    plugins: [plugin],
    options: {
      animation: animation,
      responsive: true,
      events: ['mousemove', 'mouseout', 'click', 'mouseup', 'mousedown'],

       label: {
         display: false
       },
      plugins: {
        legend: {
          events: ['click'],
          display: legendDisplay
        },
        tooltip: {
            callbacks: {
                beforeBody: (items, t) => {
                    item = items[0]
                    var resultArray = [];

                    if (stacked) {
                      var total = 0.0;
                      
                      for (index = 0; index < chart.data.datasets.length; index++) {
                        total += chart.data.datasets[index].data[item.dataIndex];
                      }
                    
                      resultArray.push("Total: " + total.toFixed(2) + "" + unit);
                    }
                    
                    return resultArray;
                },
                label: (item, t) => {
                    var resultArray = [];
                    plugin.lastElement = {label: item.label, data: item.raw, dataset: item.dataset.label, canvas: item.chart.canvas}
                    resultArray.push(`${item.dataset.label}: ${item.formattedValue}${unit}`);
                    
                    if (plugin.cagr.init !== undefined && plugin.cagr.init === true && plugin.lastElement.dataset === plugin.downElement.dataset) {
                       var multipleX = plugin.cagr.change / 100.0 + 1.0;
                       var multipleXString = " (" + multipleX.toFixed(1) + "x)";
                       
                       resultArray.push("");
                       resultArray.push( plugin.cagr.startDate + " -> " + plugin.cagr.endDate);
                       resultArray.push( "Change: " + plugin.cagr.change.toFixed(2) + "%" + multipleXString);
                       resultArray.push( "CAGR: " + plugin.cagr.cagr.toFixed(2) + "%");
                    }
                    
                    
                    return resultArray;
                }
            },
        },
        corsair: {
          color: 'black',
          continousTooltipCagr: continousTooltipCagr
        }
      },
      scales: {
        x: {
          display: true,
        },
        y: {
          display: true,
          type: 'linear',
          min: chartOptions.suggestedMin,
          max: chartOptions.suggestedMax
        }
      },
    },
  };
  if (dated) {
     chartConfig.options.scales.x = {
          display: true,
          type: 'time',
          time: {
              displayFormats: {
                  quarter: 'yyyy-MM-dd'
              },
              tooltipFormat: 'yyyy-MM-dd'
          }
      }
  }
  if (isSecondYAxisNeeded) {
     chartConfig.options.scales.y1 = {
        type: 'linear',
        display: true,
        position: 'right',

        grid: {
          drawOnChartArea: false
        },
     }
  }
  if (stacked) {
    chartConfig.options.scales.y.stacked = true;
    chartConfig.data.datasets[0].fill = '-1';
  }
  
  var chart;
  if (!isLazyLoading) {
    chart = new Chart(canvas, chartConfig);
  }
  
  var button=document.createElement("button");
  button.innerHTML = "Logarithmic";
  button.className="floatleft";
  button.onclick=function() {
      isCurrentlyLinear = chart.options.scales.y.type == 'linear';
      value = (isCurrentlyLinear ? 'logarithmic' : 'linear')
      if (isCurrentlyLinear) {
        button.classList.add("pressed");
      }else {
        button.classList.remove("pressed");
      }
      chart.options.scales.y.type = value;
      chart.update();
  }
  startAtZero = true;
  var startAtZeroButton=document.createElement("button");
  startAtZeroButton.innerHTML = "Zero based";
  startAtZeroButton.className="floatleft";
  startAtZeroButton.onclick=function() {
      isCurrentlyEnabled = (chart.options.scales.y.originalMin !== undefined);

      setStartZeroBased(isCurrentlyEnabled);

      chart.update();

      if (chartOptions.zeroBasedChangeListener !== undefined) {
        chartOptions.zeroBasedChangeListener();
      }
  }
  underChartBar.appendChild(button);
  underChartBar.appendChild(startAtZeroButton);
  
  var quarterly = defaultQuarterlyEnabled;
  if (quaterlySupported) {
    var quarterlyButton=document.createElement("button");
    quarterlyButton.innerHTML = "Quarterly";
    quarterlyButton.className="floatleft";
    if (quarterly) {
      quarterlyButton.classList.add("pressed");
    }
    quarterlyButton.onclick=function() {
        quarterly = !quarterly;
        if (quarterly) {
          quarterlyButton.classList.add("pressed");
        } else {
          quarterlyButton.classList.remove("pressed");
        }
        doUpdateChart();
    }
    underChartBar.appendChild(quarterlyButton);
  }

  
  
  if (chartOptions.slider !== undefined) {
    var sliderDiv = document.createElement("div")
    sliderDiv.className="slider-div";
    
    var valueSpan = document.createElement("span");
    
    var optionSlider = document.createElement("input");
    optionSlider.id=chartOptions.slider.id;
    optionSlider.min = chartOptions.slider.min;
    optionSlider.max = chartOptions.slider.max;
    optionSlider.value = chartOptions.slider.default;
    optionSlider.type="range";
    
    valueSpan.innerText = (chartOptions.slider.default + " " + chartOptions.slider.parameterName);
    
    optionSlider.oninput = function() {
      valueSpan.innerText = (this.value + " " + chartOptions.slider.parameterName);
      doUpdateChart();
    }
    
    sliderDiv.appendChild(optionSlider);
    sliderDiv.appendChild(valueSpan);
    
    underChartBar.appendChild(sliderDiv);
  }
  
  $(underChartBar.lastChild).removeClass("floatleft");
  


  var sliderDiv=document.createElement("div");
  sliderDiv.style="height:300px;position:relative; top: 30px;";
  var slider = $(sliderDiv).slider({
      range: true,
      orientation: "vertical",
      min: 0,
      max: 500,
      values: [ 0, 500 ],
      slide: function( event, ui ) {
          var start = ui.values[0],
          end = ui.values[1];
          
          chart.options.scales.y.min = start;
          chart.options.scales.y.max = end;
          chart.update();
      }
    });
    $(slider).on("slide", function() {
      var selection = $(this).slider("value");
    });
    chartDiv.appendChild(sliderDiv);
  chartDiv.appendChild(canvas);

  var inView = false;
  
  function isScrolledIntoView(elem)
  {
      lazyLoadOffset = 1000;

      var docViewTop = $(window).scrollTop() - lazyLoadOffset;
      var docViewBottom = $(window).scrollTop() + $(window).height() + lazyLoadOffset;
  
      var elemTop = $(canvas).offset().top;
      var elemBottom = elemTop + $(canvas).height();
  
      return ((elemTop <= docViewBottom) && (elemBottom >= docViewTop));
  }
  
  function setStartZeroBased(isCurrentlyEnabled) {
      if (isCurrentlyEnabled) {
        startAtZeroButton.classList.remove("pressed");
      } else {
        startAtZeroButton.classList.add("pressed");
      }
      if (isCurrentlyEnabled) {
        slider.slider('values', 0, chart.options.scales.y.originalMin);
        slider.slider('values', 1, chart.options.scales.y.originalMax);
        chart.options.scales.y.min = chart.options.scales.y.originalMin;
        chart.options.scales.y.max = chart.options.scales.y.originalMax;
        chart.options.scales.y.originalMin = undefined;
        chart.options.scales.y.originalMax = undefined;
      } else {
        slider.slider('values', 0, chart.options.scales.y.min);
        slider.slider('values', 1, chart.options.scales.y.max);
        chart.options.scales.y.originalMin = chart.options.scales.y.min;
        chart.options.scales.y.originalMax = chart.options.scales.y.max;
        if (chart.options.scales.y.min > 0) {
           chart.options.scales.y.min = 0;
        }
        if (chart.options.scales.y.max < 0) {
          chart.options.scales.y.max = 0;
        }
      }
  }

  function doUpdateChart() {
        xValues.length = 0;
        yValues.length = 0;
        
        /*
        if (chart.data.datasets.length > 2) {
          while (chart.data.datasets.length > 1) {
            chart.data.datasets.shift();
          }
        }*/
        numberOfAdditionalCharts = chartOptions.additionalCharts !== undefined ? chartOptions.additionalCharts.length + 1 : 1;
        while (chart.data.datasets.length < numberOfAdditionalCharts) {
          chart.data.datasets.unshift({});
        }
        
        
        let url = (addStockPrefix ? '/' + stockToLoad : "") + urlPath;
        var parameters = new Map();
        var parameterString = "";
        
        if (quarterly) {
          parameters.set('quarterly', 'true');
        }
        if (chartOptions.slider !== undefined) {
          parameters.set(chartOptions.slider.parameterName, $(optionSlider).val());
        }
        if (chartOptions.staticParameters !== undefined) {
          for (let [key, value] of Object.entries(chartOptions.staticParameters)) {
            parameters.set(key, value);
          }
        }
        
        i = 0;
        for (let [key, value] of parameters) {
          parameterString += (key + "=" + value);
          if (i < parameters.length - 1) {
            parameterString += "&";
          }
          ++i;
        }
        if (parameterString.length > 0) {
          url += "?" + parameterString;
        }
        console.log("Fetching: " + url);
        fetch(url)
          .then(res => res.json())
          .then(out => {
                      for (index = 0; index < out.length; index++) {
                          xValues[out.length - index - 1] = out[index].date;
                          yValues[out.length - index - 1] = out[index].value;
                      }
                      for (index = 0; index < additionalLabelsAtEnd.length; index++) {
                          xValues.push(additionalLabelsAtEnd[index]);
                      }
                      max = Math.max.apply(Math, yValues);
                      min = Math.min.apply(Math, yValues);
                  
                      if (min < 0) {
                        min *= 1.04;
                      }
                      if (max < 0) {
                        max *= 0.96;
                      }
                      if (min > 0) {
                        min *= 0.96;
                      }
                      if (max > 0) {
                        max *= 1.04;
                      }
                      if (Math.abs(max - min) < 0.001) {
                         center = min;
                         max = center + 1.0;
                         min = center - 1.0;
                      }
                      if (min > max) {
                         max = min + 1.0;
                      }
                  
                      minValueToSet = chartOptions.suggestedMin !== undefined ? chartOptions.suggestedMin : min;
                      maxValueToSet = chartOptions.suggestedMax !== undefined ? chartOptions.suggestedMax : max;
                      
                      minValueToSet = minValueToSet < min ? min : minValueToSet;
                      maxValueToSet = maxValueToSet > max ? max : maxValueToSet;
                      

                      console.log(url + " " + min + " " + max + " " + minValueToSet + " " + maxValueToSet);
                      if (!isNaN(min)) {
                        slider.slider("option", "min", min);
                      }
                      if (!isNaN(max)) {
                        slider.slider("option", "max", max);
                      }
                      if (!isNaN(minValueToSet)) {
                        chart.options.scales.y.min = minValueToSet;
                      }
                      if (!isNaN(maxValueToSet)) {
                        chart.options.scales.y.max = maxValueToSet;
                      }
                      setStartZeroBased(false);
                      chart.update();
          }).then(out => {
                   if (chartOptions.additionalCharts !== undefined && chartOptions.additionalCharts.length > 0) {
                     for (elementIndex in chartOptions.additionalCharts) {
                        var element = chartOptions.additionalCharts[elementIndex];
                        chartToUpdate = (chartOptions.additionalCharts.length == 1 && chart.data.datasets[0].label !== undefined) ? 0 : -1;
                        addAdditionalChart(element, parameterString, chartToUpdate, parseInt(elementIndex));
                     }
                   
                   }
                   chart.update();
                   if (chartOptions.runAfter !== undefined) {
                     chartOptions.runAfter();
                   }
                   chart.options.animation = true; // enable animation after display
          })
          .catch(err => { throw err });
    
    
    function addAdditionalChart(element, parameterString, chartToUpdate, elementIndex) {
          localUri  = (addStockPrefix ? '/' + stockToLoad : "") + element.url;
          
          if (parameterString.length > 0) {
            localUri += "?" + parameterString;
          }
          fetch(localUri)
            .then(res => res.json())
            .then(out => {
                        var newXValues = [];
                        var newYValues = [];
                        var updateSecondAxis = isSecondYAxisNeeded && elementIndex == 1;
                        if (chartToUpdate != -1) {
                          newYValues = chart.data.datasets[chartToUpdate].data;
                          newXValues = chart.data.datasets[chartToUpdate].label;
                        }
                        console.log(newXValues);
                        for (index = 0; index < out.length; index++) {
                            newXValues[out.length - index - 1] = out[index].date;
                            newYValues[out.length - index - 1] = out[index].value;
                        }
                        if (!isSecondYAxisNeeded) {
                          max = Math.max.apply(Math, newYValues);
                          min = Math.min.apply(Math, newYValues);
                      
                          for (i = 0; i < elementIndex; ++i) {
                            max2 = Math.max.apply(Math, chart.data.datasets[i].data);
                            min2 = Math.min.apply(Math, chart.data.datasets[i].data);
                        
                            if (max2 > max) {
                              max = max2;
                            }
                            if (min2 < min) {
                              min = min2;
                            }
                          }
                          if (min < 0) {
                            minValueToSet = min * 1.07;
                          }
                          if (max < 0) {
                            maxValueToSet = max * 0.93;
                          }
                          if (min > 0) {
                            minValueToSet = min * 0.93;
                          }
                          if (max > 0) {
                            maxValueToSet = max * 1.07;
                          }
                      
                          if (chart.options.scales.y.min < minValueToSet) {
                             minValueToSet = chart.options.scales.y.min;
                          }
                          if (chart.options.scales.y.max > maxValueToSet) {
                             maxValueToSet = chart.options.scales.y.max;
                          }
                          if (chartOptions.suggestedMin !== undefined && minValueToSet < chartOptions.suggestedMin) {
                             minValueToSet = chartOptions.suggestedMin;
                          }
                          if (chartOptions.suggestedMax !== undefined && chartOptions.suggestedMax < maxValueToSet) {
                             maxValueToSet = chartOptions.suggestedMax;
                          }
                          
  
                          
                          if (minValueToSet >= maxValueToSet) {
                             minValueToSet = maxValueToSet + 1.0;
                          }
                          yAxisID='y';
                        } else {
                           yAxisID='y1';
                        }
                        if (chartToUpdate == -1) {
                          console.log(elementIndex + " " + colorPalette.length);
                          shouldFill = chartConfig.options.scales.y.stacked === true;
                          var newChart = {
                            pointRadius: 2,
                            borderColor: colorPaletteLine[(elementIndex + 1) % colorPalette.length],
                            backgroundColor: colorPalette[(elementIndex + 1) % colorPalette.length],
                            data: newYValues,
                            pointHitRadius: 300,
                            label: element.label,
                            yAxisID: yAxisID,
                            fill: false
                          };
                          
                          console.log(newChart.backgroundColor + " " + (elementIndex + 1) + " % " + colorPalette.length + " = " + ((elementIndex + 1) % colorPalette.length) + " " + typeof(elementIndex));
                          
                          isLast = elementIndex == 0;

                          if (shouldFill) {
                            newChart.fill = '-1';
                          }
                          if (isLast && shouldFill) {
                            newChart.fill = true;
                          }
                          
                          chart.data.datasets[elementIndex] = newChart;
                        }

                        if (!isNaN(minValueToSet)) {
                          slider.slider('values', 0, minValueToSet);
                          if (chart.options.scales.y.originalMin != undefined) {
                            chart.options.scales.y.originalMin = minValueToSet;
                          }
                          chart.options.scales.y.min = minValueToSet;
                        }
                        if (!isNaN(maxValueToSet)) {
                          if (chart.options.scales.y.originalMax != undefined) {
                            chart.options.scales.y.originalMax = maxValueToSet;
                          }
                          slider.slider('values', 1, maxValueToSet);
                          chart.options.scales.y.max = maxValueToSet;
                        }
                        chart.update();
            })
            .catch(err => { throw err });
    }
  }

  function updateFunction() {
    if (isScrolledIntoView(canvas) || !isLazyLoading) {
        if (inView) { return; }
        inView = true;
        if (chart == null) {
          chart = new Chart(canvas, chartConfig);
        }
        doUpdateChart();
  }};


  $(window).scroll(updateFunction);
  updateFunction();

  return chart;
    

}



function createBubbleChart(url, title, chartOptions) {
    var canvas =document.createElement("canvas");
    canvas.style='width:100%;max-height:600px'
    
    var chart = undefined;
    
    var underChartBar = document.createElement("div");
    underChartBar.style="width:100%";
    
    var chartDiv = document.createElement("div");
    chartDiv.className="chartDiv";
    
    chartDiv.appendChild(canvas);
    
    var titleDiv = document.createElement("h2");
    titleDiv.innerText = title;
    titleDiv.className="chart-title";

    var element = document.getElementById("charts");
    element.appendChild(titleDiv);
    element.appendChild(chartDiv);
    element.appendChild(underChartBar);


    fetch(url)
      .then(res => res.json())
      .then(out => {
              const data = {
                datasets: [{
                  label: 'First Dataset',
                  data: out,
                  backgroundColor: 'rgba(0,0,255,0.8)'
                }]
              };
              
              const bubbleConfig = {
                type: 'bubble',
                data: data,
                options: {
                  plugins: {
                      annotation: {
                        annotations: [{
                              drawTime: "beforeDraw",
                              type: 'line',
                              yMin: 0,
                              yMax: 0,
                              borderColor: 'rgb(255, 99, 132)',
                              borderWidth: 4, 
                        }]
                    },
                    tooltip: {
                          callbacks: {
                              label: (item) => {
                                  return `${item.raw.description}`
                              }
                          },
                      },
                  }
                },
              };
              chart = new Chart(canvas, bubbleConfig);
              
              chart.update();
      })
      .catch(err => { throw err });
      
      if (chartOptions.slider !== undefined) {
        var sliderDiv = document.createElement("div")
        sliderDiv.className="slider-div";
        
        var valueSpan = document.createElement("span");
        
        var optionSlider = document.createElement("input");
        optionSlider.id=chartOptions.slider.id;
        optionSlider.min = chartOptions.slider.min;
        optionSlider.max = chartOptions.slider.max;
        optionSlider.value = chartOptions.slider.default;
        optionSlider.type="range";
        
        valueSpan.innerText = (chartOptions.slider.default + " " + chartOptions.slider.parameterName);
        
        optionSlider.oninput = function() {
          valueSpan.innerText = (this.value + " " + chartOptions.slider.parameterName);
          fetch(url + "?" + chartOptions.slider.parameterName + "=" + this.value)
              .then(res => res.json())
              .then(out => {
                    chart.data.datasets[0].data = out;
                    chart.update();
                });
          }
          
          sliderDiv.appendChild(optionSlider);
          sliderDiv.appendChild(valueSpan);
          
          underChartBar.appendChild(sliderDiv);
        }
  }

