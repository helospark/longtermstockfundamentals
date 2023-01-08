    function refreshJwt() {
      fetch('/user/jwt/refresh?force=true', {
            method: 'POST',
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            }
          }).then(() => {
            location.reload();
          });
      }
      
    
    function deleteAccount() {
        accountType = $("#login-account-type").text();
        
        additionalText = "";
        
        if (accountType != "FREE") {
          additionalText += "<br/><b>Removing account will cancel your current subscription.<br/>If you would like to cancel your subscription you can do that without deleting your account by clicking \"Downgrade\" on the plans panel under the free plan.</b>";
        }
        
        $("#generic-modal .modal-content")
        $("#generic-modal .modal-content").html(`
            <div class="modal-header">
                <h5 class="modal-title">Delete account</h5>
                <button type="button" class="btn-close" data-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body" id="modal-body">
                We are sad to see you go!<br/>
                Once you click delete, this action cannot be undone.<br>
                ` + additionalText + `<br/><br/>
                For additional security, please provide your password:
                <input class="form-control" id="delete-account-password" type="password"></input>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-success" data-dismiss="modal">Cancel</button>
                <button id="delete-account-button" type="button" class="btn btn-danger">Delete</button>
            </div>`);
        modal = new bootstrap.Modal(document.getElementById("generic-modal"));
        modal.show();
    
        $("#delete-account-button").click(function() {
              password = $("#delete-account-password").val();
              fetch('/user/delete', {
                      method: 'POST',
                      headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                      },
                      body: JSON.stringify({password: password})
                  })
                  .then(async function(res) {
                      json = await res.json();
                      redirectTo="";
                      if (res.status == 200) {
                        result = "Account successfully removed";
                        redirectTo = "/";
                      } else {
                        result = json.errorMessage;
                        redirectTo = "/profile";
                      }

                      $("#generic-modal .modal-content").html(`
                          <div class="modal-header">
                              <h5 class="modal-title">Delete account</h5>
                              <button type="button" class="btn-close" data-dismiss="modal" aria-label="Close"></button>
                          </div>
                          <div class="modal-body" id="modal-body">
                              ` + result + `
                          </div>
                          <div class="modal-footer">
                              <button type="button" class="btn btn-success" data-dismiss="modal">Close</button>
                          </div>`);
                      $("#generic-modal").on("hide.bs.modal",  () => window.location.href=redirectTo);
                  });
        });
     }
     
     function changePassword() {
              oldPassword = $("#old_password").val();
              newPassword = $("#new_password").val();
              newPasswordVerify = $("#new_password_verify").val();
              fetch('/user/password', {
                      method: 'POST',
                      headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                      },
                      body: JSON.stringify({oldPassword: oldPassword, newPassword: newPassword, newPasswordVerify: newPasswordVerify})
                  })
                  .then(async function(res) {
                      json = await res.json();
                      if (res.status == 200) {
                        result = "Password successfully changed!";
                      } else {
                        result = json.errorMessage;
                      }

                      $("#generic-modal .modal-content").html(`
                          <div class="modal-header">
                              <h5 class="modal-title">Change password</h5>
                              <button type="button" class="btn-close" data-dismiss="modal" aria-label="Close"></button>
                          </div>
                          <div class="modal-body" id="modal-body">
                              ` + result + `
                          </div>
                          <div class="modal-footer">
                              <button type="button" class="btn btn-success" data-dismiss="modal">Close</button>
                          </div>`);
                      modal = new bootstrap.Modal(document.getElementById("generic-modal"));
                      modal.show();
                      $("#generic-modal").on("hide.bs.modal",  () => location.reload());
                  });
     }