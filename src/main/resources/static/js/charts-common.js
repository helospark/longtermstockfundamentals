var constColorPalette = [
  "rgba(0,0,255,0.3)",
  "rgba(255,0,0,0.3)",
  "rgba(75,200,32,0.3)",
];
var constColorPaletteLine = [
  "rgba(0,0,255,0.8)",
  "rgba(255,0,0,0.8)",
  "rgba(75,200,32,0.8)",
];

function createCheckbox(label) {
  var checkbox = document.createElement("input");
  checkbox.type="checkbox";
  checkbox.innerHtml=label;
  

  return label;
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
  
  var colorPaletteLine = constColorPaletteLine;
  var colorPalette = constColorPalette;
  
  if (type == "bar") {
    colorPalette = ["rgba(0,0,255,0.6)"];
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
    options: {
      animation: animation,
      responsive: true,

       label: {
         display: false
       },
      plugins: {
        /*title: {
          display: true,
          text: title
        },*/
        legend: {
          display: legendDisplay
        },
        tooltip: {
            callbacks: {
                label: (item) =>
                    `${item.dataset.label}: ${item.formattedValue}${unit}`,
            },
        },
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
        
        if (chart.data.datasets.length > 2) {
          while (chart.data.datasets.length > 1) {
            chart.data.datasets.shift();
          }
        }
        
        let url = '/' + stockToLoad + urlPath;
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
                  
                      minValueToSet = chartOptions.suggestedMin !== undefined ? chartOptions.suggestedMin : min;
                      maxValueToSet = chartOptions.suggestedMax !== undefined ? chartOptions.suggestedMax : max;
                      
                      minValueToSet = minValueToSet < min ? min : minValueToSet;
                      maxValueToSet = maxValueToSet > max ? max : maxValueToSet;
                      
                      if (minValueToSet >= maxValueToSet) {
                         minValueToSet = maxValueToSet + 1.0;
                      }

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
                        chartToUpdate = (chartOptions.additionalCharts.length == 1 && chart.data.datasets.length == 2) ? 0 : -1;
                        addAdditionalChart(element, parameterString, chartToUpdate);
                     }
                   
                   }
                   chart.update();
                   if (chartOptions.runAfter !== undefined) {
                     chartOptions.runAfter();
                   }
                   chart.options.animation = true; // enable animation after display
          })
          .catch(err => { throw err });
    
    
    function addAdditionalChart(element, parameterString, chartToUpdate) {
          localUri  = '/' + stockToLoad + element.url;
          
          if (parameterString.length > 0) {
            localUri += "?" + parameterString;
          }
          fetch(localUri)
            .then(res => res.json())
            .then(out => {
                        var newXValues = [];
                        var newYValues = [];
                        var updateSecondAxis = isSecondYAxisNeeded && chart.data.datasets.length == 1;
                        if (chartToUpdate != -1) {
                          newYValues = chart.data.datasets[chartToUpdate].data;
                          newXValues = chart.data.datasets[chartToUpdate].label;
                        }
                        for (index = 0; index < out.length; index++) {
                            newXValues[out.length - index - 1] = out[index].date;
                            newYValues[out.length - index - 1] = out[index].value;
                        }
                        if (!isSecondYAxisNeeded) {
                          max = Math.max.apply(Math, newYValues);
                          min = Math.min.apply(Math, newYValues);
                      
                          for (i = 0; i < chart.data.datasets.length; ++i) {
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
                          chart.data.datasets.unshift({
                            pointRadius: 2,
                            borderColor: colorPaletteLine[chart.data.datasets.length % colorPalette.length],
                            backgroundColor: colorPalette[chart.data.datasets.length % colorPalette.length],
                            data: newYValues,
                            pointHitRadius: 300,
                            label: element.label,
                            yAxisID: yAxisID
                          });
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
