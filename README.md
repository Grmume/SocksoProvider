# SocksoProvider
Sockso-Provider for the Encore Mediaplayer

This is a prototype for a music provider for the android encore mediaplayer. It is still a prototype and no usable in the current state.

Todo:
  - Async data fetching  
      Return the dataobject directly but set isLoaded to false to tell the mediaplayer that the data is not completely loaded
  - Implement Playback  
      The mp3 data is fetched from the sockso server, but the conversion and playback of the pcm samples fails  
      The mp3 data is decoded using the MediaCodec interface, but the returned pcm data cannot be played (neither by android nor on the pc)
  - Close the settings screen when pressing the back arrow on the main page
  - Remove excessesive debug output
    
