if (typeof autoCompleteJS === 'undefined') {
const autoCompleteJS = new autoComplete({
    placeHolder: "Search for stocks...",
    data: {
        src: async (query) => {
          let url = "/suggest?search=" + query;
          
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
                uriPart = $("#topnav").attr("data-uri");
                if ( !(uriPart == "stock" || uriPart == "calculator")) { // Uri not supporting stock path params
                  uriPart = "stock";
                }
                const selection = event.detail.selection.value;
                window.location.href = "/" + uriPart + "/" + selection.key
            }
        }
    }
});
}
