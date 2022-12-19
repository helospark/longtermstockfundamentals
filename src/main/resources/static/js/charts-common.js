var constColorPalette = [
  "rgba(0,0,255,0.3)",
  "rgba(255,0,0,0.3)",
  "rgba(0,255,0,0.3)",
];
var constColorPaletteLine = [
  "rgba(0,0,255,0.8)",
  "rgba(255,0,0,0.8)",
  "rgba(0,255,0,0.8)",
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
  
  var titleDiv = document.createElement("h4");
  titleDiv.innerText = title;
  titleDiv.className="chart-title";
  
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
  
  var colorPaletteLine = constColorPaletteLine;
  var colorPalette = constColorPalette;
  
  if (type == "bar") {
    colorPalette = ["rgba(0,0,255,0.6)"];
  }
  
  var additionalLabelsAtEnd = chartOptions.additionalLabelsAtEnd === undefined ? [] : chartOptions.additionalLabelsAtEnd;
  
  var chart = new Chart(canvas, {
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
        label: label
      }]
    }, 
    options: {
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
  });
  
  
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
  var startAtZeroButton=document.createElement("button");
  startAtZeroButton.innerHTML = "Zero based";
  startAtZeroButton.className="floatleft";
  startAtZeroButton.onclick=function() {
      isCurrentlyEnabled = (chart.options.scales.y.originalMin !== undefined);
      if (isCurrentlyEnabled) {
        startAtZeroButton.classList.remove("pressed");
      } else {
        startAtZeroButton.classList.add("pressed");
      }
      if (isCurrentlyEnabled) {
        chart.options.scales.y.min = chart.options.scales.y.originalMin;
        chart.options.scales.y.max = chart.options.scales.y.originalMax;
        chart.options.scales.y.originalMin = undefined;
        chart.options.scales.y.originalMax = undefined;
      } else {
        chart.options.scales.y.originalMin = chart.options.scales.y.min;
        chart.options.scales.y.originalMax = chart.options.scales.y.max;
        if (chart.options.scales.y.min > 0) {
           chart.options.scales.y.min = 0;
        }
        if (chart.options.scales.y.max < 0) {
          chart.options.scales.y.max = 0;
        }
      }

      chart.update();
  }
  underChartBar.appendChild(button);
  underChartBar.appendChild(startAtZeroButton);

  if (chartOptions.slider !== undefined) {
    var sliderDiv = document.createElement("div")
    var optionSlider = document.createElement("input");
    optionSlider.id=chartOptions.slider.id;
    optionSlider.min = chartOptions.slider.min;
    optionSlider.max = chartOptions.slider.max;
    optionSlider.value = chartOptions.slider.default;
    optionSlider.type="range";
    
    optionSlider.oninput = function() {
    
      let paramedUrl = 'http://localhost:8080/' + stock + urlPath + "?" + chartOptions.slider.parameterName + "=" + this.value;
  
      fetch(paramedUrl)
          .then(res => res.json())
          .then(out => {
                      for (index = 0; index < out.length; index++) {
                          xValues[out.length - index - 1] = out[index].date;
                          yValues[out.length - index - 1] = out[index].value;
                      }
                      chart.update();
          })
          .catch(err => { throw err });
    }
    
    var sliderLabel = document.createElement("label");
    sliderLabel.htmlFor=chartOptions.slider.id;
    sliderLabel.innerText=chartOptions.slider.parameterName;
    
    sliderDiv.appendChild(sliderLabel);
    sliderDiv.appendChild(optionSlider);
    
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
  
  
  let url = 'http://localhost:8080/' + stock + urlPath;
  
  fetch(url)
          .then(res => res.json())
          .then(out => {
                      for (index = 0; index < out.length; index++) {
                          xValues[out.length - index - 1] = out[index].date;
                          yValues[out.length - index - 1] = out[index].value;
                      }
                      for (index = 0; index < additionalLabelsAtEnd.length; index++) {
                          xValues.push(additionalLabelsAtEnd[index]);
                         // yValues.push(1.0);
                      }
                      max = Math.max.apply(Math, yValues);
                      min = Math.min.apply(Math, yValues);
                  
                      minValueToSet = chartOptions.suggestedMin !== undefined ? chartOptions.suggestedMin : min;
                      maxValueToSet = chartOptions.suggestedMax !== undefined ? chartOptions.suggestedMax : max;
                      
                      minValueToSet = minValueToSet < min ? min : minValueToSet;
                      maxValueToSet = maxValueToSet > max ? max : maxValueToSet;
                      
                      if (minValueToSet >= maxValueToSet) {
                         minValueToSet = maxValueToSet + 1.0;
                      }
                  
                      if (!isNaN(minValueToSet)) {
                        slider.slider("option", "min", min);
                        slider.slider('values', 0, minValueToSet);
                        chart.options.scales.y.min = minValueToSet;
                      }
                      if (!isNaN(maxValueToSet)) {
                        slider.slider("option", "max", max);
                        slider.slider('values', 1, maxValueToSet);
                        chart.options.scales.y.max = maxValueToSet;
                      }
                      startAtZeroButton.onclick();
                      chart.update();
          }).then(out => {
                   if (chartOptions.additionalCharts !== undefined && chartOptions.additionalCharts.length > 0) {
                     for (elementIndex in chartOptions.additionalCharts) {
                        var element = chartOptions.additionalCharts[elementIndex];
                        addAdditionalChart(element);
                     }
                   
                   }
                   if (chartOptions.runAfter !== undefined) {
                     chartOptions.runAfter();
                   }
          })
          .catch(err => { throw err });
    
    
    function addAdditionalChart(element) {
          localUri  = 'http://localhost:8080/' + stock + element.url;
          fetch(localUri)
            .then(res => res.json())
            .then(out => {
                        var newXValues = [];
                        var newYValues = [];
                        for (index = 0; index < out.length; index++) {
                            newXValues[out.length - index - 1] = out[index].date;
                            newYValues[out.length - index - 1] = out[index].value;
                        }
                        max = Math.max.apply(Math, newYValues);
                        min = Math.min.apply(Math, newYValues);
                    
                        minValueToSet = min;
                        maxValueToSet = max;
                    
                        if (chart.options.scales.y.min < minValueToSet) {
                           minValueToSet = chart.options.scales.y.min;
                        }
                        if (chart.options.scales.y.max > maxValueToSet) {
                           maxValueToSet = chart.options.scales.y.max;
                        }

                        
                        if (minValueToSet >= maxValueToSet) {
                           minValueToSet = maxValueToSet + 1.0;
                        }
                        chart.data.datasets.unshift({
                          pointRadius: 2,
                          borderColor: colorPaletteLine[chart.data.datasets.length % colorPalette.length],
                          backgroundColor: colorPalette[chart.data.datasets.length % colorPalette.length],
                          data: newYValues,
                          pointHitRadius: 300,
                          label: element.label
                        });
                    
                        if (!isNaN(minValueToSet)) {
                          slider.slider("option", "min", min);
                          slider.slider('values', 0, minValueToSet);
                          chart.options.scales.y.min = minValueToSet;
                        }
                        if (!isNaN(maxValueToSet)) {
                          slider.slider("option", "max", max);
                          slider.slider('values', 1, maxValueToSet);
                          chart.options.scales.y.max = maxValueToSet;
                        }
                        chart.update();
            })
            .catch(err => { throw err });
    }
    
  return chart;
    

}
