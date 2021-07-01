console.log("Javascript is ready to go!");

var bucketRegion = "ap-south-1";
var s3;

var bucketName = "direct-upload-bucket";
var destBucketName = "files-encrypted";
var IdentityPoolId = "";

var bucketName2 = "new-bucket-upload";
var destBucketName2 = "new-bucket-download";
var IdentityPoolId2 = "";


function getEventTarget(e) {
    e = e || window.event;
    return e.target || e.srcElement;
}

function s3download2(key){
  if(!key) return;
  console.log(key);

  AWS.config.update({
          region: bucketRegion,
          credentials: new AWS.CognitoIdentityCredentials({
              IdentityPoolId: IdentityPoolId2
          })
  });

  s3 = new AWS.S3({
        apiVersion: '2006-03-01',
        params: {Bucket: "download-trigger"}
  });

  if (key){
    s3.upload({
        Key: key,
        ContentType: 'String',
        Body: String(key),
        ACL: 'public-read'
        }, function(err, data) {
        if(err) {
        reject('error');
    }

    if(!alert('Download the file here: https://open-download-bucket.s3.ap-south-1.amazonaws.com/'+key)){
      $("#progress-down").attr('value', 0);}
        }).on('httpUploadProgress', function (progress) {
        var uploaded = parseInt((progress.loaded * 100) / progress.total);
        $("#progress-down").attr('value', uploaded);
      });
  }
}

function s3download(key){
  if(!key) return;
  console.log(key);

  AWS.config.update({
          region: bucketRegion,
          credentials: new AWS.CognitoIdentityCredentials({
              IdentityPoolId: IdentityPoolId
          })
  });

  s3 = new AWS.S3({
        apiVersion: '2006-03-01',
        params: {Bucket: "to-download-files"}
  });

  if (key){
    s3.upload({
        Key: key,
        ContentType: 'String',
        Body: String(key),
        ACL: 'public-read'
        }, function(err, data) {
        if(err) {
        reject('error');
    }

    if(!alert('Download the file here: https://open-download-bucket.s3.ap-south-1.amazonaws.com/'+key)){
      $("#progress-down").attr('value', 0);}
        }).on('httpUploadProgress', function (progress) {
        var uploaded = parseInt((progress.loaded * 100) / progress.total);
        $("#progress-down").attr('value', uploaded);
      });
  }

}


$(document).ready(function(){

  $("#list_kms").click(function(){

      AWS.config.update({
              region: bucketRegion,
              credentials: new AWS.CognitoIdentityCredentials({
                  IdentityPoolId: IdentityPoolId
              })
      });

      s3 = new AWS.S3({apiVersion: '2006-03-01'});
      var bucketParams = {Bucket : destBucketName};

      s3.listObjects(bucketParams, function(err, data) {
        if (err) {
          console.log("Error", err);
        }else{
          console.log("Success");
          var mlist = data['Contents']

          var str = '<ul class="item list-group list-group-flush">'
          mlist.forEach(function(l) {
            str += '<a class="list-group-item">'+ l['Key'] + '</a>';
          });

          str += '</ul>';
          document.getElementById("downloadContainer").innerHTML = str;


          $(".item").click(function(event){
            var target = getEventTarget(event);
            var key = target.innerHTML;
            s3download(key);
          });

        }});

  });

  $("#list_env").click(function(){
    AWS.config.update({
            region: bucketRegion,
            credentials: new AWS.CognitoIdentityCredentials({
                IdentityPoolId: IdentityPoolId2
            })
    });

    s3 = new AWS.S3({apiVersion: '2006-03-01'});
    var bucketParams = {Bucket : destBucketName2};

    s3.listObjects(bucketParams, function(err, data) {
      if (err) {
        console.log("Error", err);
      }else{
        console.log("Success");
        var mlist = data['Contents']

        var str = '<ul class="item list-group list-group-flush">'
        mlist.forEach(function(l) {
          str += '<a class="list-group-item">'+ l['Key'] + '</a>';
        });

        str += '</ul>';
        document.getElementById("downloadContainer").innerHTML = str;


        $(".item").click(function(event){
          var target = getEventTarget(event);
          var key = target.innerHTML;
          s3download2(key);
        });

    }});
  });

  $("#u_kms").click(function(){
    AWS.config.update({
            region: bucketRegion,
            credentials: new AWS.CognitoIdentityCredentials({
                IdentityPoolId: IdentityPoolId
            })
    });

    s3 = new AWS.S3({
          apiVersion: '2006-03-01',
          params: {Bucket: bucketName}
    });

   var files = document.getElementById('formFileLg').files;
   if (files)
   {
     var file = files[0];
     var fileName = file.name;
     var fileUrl = 'https://' + bucketRegion + '.amazonaws.com/' +  fileName;
     s3.upload({
        Key: fileName,
        Body: file,
        ACL: 'public-read'
        }, function(err, data) {
        if(err) {
        reject('error');
        }
        // alert('Successfully Uploaded!');
        if(!alert('Successfully Uploaded !')){window.location.reload();}
        }).on('httpUploadProgress', function (progress) {
        var uploaded = parseInt((progress.loaded * 100) / progress.total);
        $("#progress-up").attr('value', uploaded);
      });
   }
  });

  $("#u_env").click(function(){

        AWS.config.update({
                region: bucketRegion,
                credentials: new AWS.CognitoIdentityCredentials({
                    IdentityPoolId: IdentityPoolId2
                })
        });

        s3 = new AWS.S3({
              apiVersion: '2006-03-01',
              params: {Bucket: bucketName2}
        });

       var files = document.getElementById('formFileLg').files;
       if (files)
       {
         var file = files[0];
         var fileName = file.name;
         var fileUrl = 'https://' + bucketRegion + '.amazonaws.com/' +  fileName;
         s3.upload({
            Key: fileName,
            Body: file,
            ACL: 'public-read'
            }, function(err, data) {
            if(err) {
            reject('error');
            }
            // alert('Successfully Uploaded!');
            if(!alert('Successfully Uploaded !')){window.location.reload();}
            }).on('httpUploadProgress', function (progress) {
            var uploaded = parseInt((progress.loaded * 100) / progress.total);
            $("#progress-up").attr('value', uploaded);
          });
       }
  });

});
