createdCharts = new Map();

     function addPieChart(pieChart, id, title, usePerformanceColorScheme=false) {
           previousChart = createdCharts.get(id);
           if (previousChart != null) {
             previousChart.destroy();
           }
           if (usePerformanceColorScheme) {
             backgroundColor = [ "green", "lime", "black", "grey", "orange", "red"];
           } else {
             backgroundColor = [ "green", "red", "maroon", "purple", "lime", "olive", "yellow", "navy", "blue", "teal", "aqua"];
           }
           const data = {
              labels: pieChart.keys,
              datasets: [{
                label: title,
                data: pieChart.values,
                hoverOffset: 4,
                backgroundColor: backgroundColor
              }],
          };
          const config = {
            type: 'pie',
            data: data,
            options: {
                plugins: {
                    legend: {
                        display: false
                    },
                    title: {
                        display: true,
                        text: title,
                        font: {
                            size: 16,
                            weight: 'bold'
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                let label = context.label;
                                let value = context.raw;
                
                                if (!label)
                                    label = 'Unknown'
                
                                let sum = 0;
                                let dataArr = pieChart.values;
                                dataArr.map(data => {
                                    sum += Number(data);
                                });
                 
                                let percentage = (value * 100 / sum).toFixed(2) + '% ($' + value.toFixed(0).toLocaleString() + ")";
                                return label + ": " + percentage;
                            }
                        }
                     }
                    }
                }
          };
          
          chart=new Chart($(id), config);
          
          createdCharts.set(id, chart);
     }
     
     function addPortfolio(data, options={}) {
           addPieChart(data.investments, "#investments-chart", "Investments");
           addPieChart(data.industry, "#industry-chart", "Industry");
           addPieChart(data.sector, "#sector-chart", "Sector");
           addPieChart(data.cap, "#cap-chart", "Marketcap");
           addPieChart(data.country, "#country-chart", "Country");
           addPieChart(data.profitability, "#profitability-chart", "Profitability");
           addPieChart(data.peChart, "#pe-chart", "PE");
           
           addPieChart(data.roicChart, "#roic-chart", "ROIC", true);
           addPieChart(data.altmanChart, "#altman-chart", "AltmanZ", true);
           addPieChart(data.growthChart, "#growth-chart", "Growth", true);
           addPieChart(data.icrChart, "#icr-chart", "ICR", true);
           addPieChart(data.grossMarginChart, "#grossmargin-chart", "Gross margin", true);
           addPieChart(data.shareChangeChart, "#sharechange-chart", "Share change", true);
           addPieChart(data.piotroskyChart, "#piotrosky-chart", "Piotrosky", true);


           
           watchlistHtml = createWatchlistTableHtml(data.columns, data.portfolio, false);
           $("#watchlist-table").html(watchlistHtml);
           
           dataTableConfig={
              paging: false,
             "iDisplayLength": 100
           };
           
           if (options.reorder === true) {
             dataTableConfig.order=[ '3', 'desc' ]
           }
           
           $('#watchlist-table table').DataTable(dataTableConfig);
           if (data.portfolio.length == 0) {
             $("#watchlist-table").html("<h3>Add something to your watchlist with owned shares greater than 0 to see your portfolio details.</h3>" + $("#watchlist-table").html());
           }


           returnsWatchlistHtml = createWatchlistTableHtml(data.returnsColumns, data.returnsPortfolio, false);
           $("#returns-table").html(returnsWatchlistHtml);
           $('#returns-table table').DataTable({
              paging: false,
             "iDisplayLength": 100,
             "order": [ '2', 'desc' ]
           });
     }