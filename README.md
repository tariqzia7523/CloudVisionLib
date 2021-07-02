# CloudVisionLib


This lib used to get text from cloud vision lib. you only have to donwload it and as add it as module  and on main activity call follwoing lines. 

 ApplyOCR(this@MainActivity, "Your_api_key", imageUri!!, object : OnOcrResponseListener {
 
                override fun ocrSuccess(string: String) {
                
                    
                  Log.e("****TAG","value is "+string)
               
               }
                
                override fun ocrFailure() {
                    
                    Log.e("****TAG","value is "+"failure")
                
                }
            
            }).start()
