import Flutter
import UIKit
import AVFoundation

public class SwiftFileaudioplayerPlugin: NSObject, FlutterPlugin {
    
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "fileaudioplayer", binaryMessenger: registrar.messenger())
        let instance = SwiftFileaudioplayerPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    private var players : [String: Player] = [:];
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        //flutterResult = result
        
        let action = call.method
        guard let args = call.arguments else {
            print("no args")
            result(FlutterError(code: "-1", message: "no args", details: nil))
            return
        }
        if let myArgs = args as? [String:Any],
            let channel = myArgs["channel"] as? String {
            let player = players[channel] ?? Player();
            players[channel] = player

            if (action == "start") {
                if let url = myArgs["url"] as? String {
                    player.start(filePath : url, result: result)
                } else {
                    print("no file")
                    result(FlutterError(code: "-1", message: "no file", details: nil))
                }
            } else if (action == "stop") {
                player.stop(result: result)
            } else if (action == "pause") {
                player.pause(result: result)
            } else {
                player.resume(result: result)
            }
        } else {
            result(FlutterError(code: "-1", message: "iOS could not extract " +
                 "flutter arguments in method: (sendParams)", details: nil))
            
        }
    }
}

public class Player:  NSObject, AVAudioPlayerDelegate {
    private var audioPlayer: AVAudioPlayer?
    private var flutterResult: FlutterResult?

    public func start(filePath: String, result: @escaping FlutterResult){
        let url = URL(string: filePath)
        flutterResult = result
        let avopts:AVAudioSession.CategoryOptions  = [
		    .mixWithOthers,
		    .duckOthers,
		    .interruptSpokenAudioAndMixWithOthers
	    ]
        
        if (url != nil){
            do {
                try AVAudioSession.sharedInstance().setCategory(AVAudioSession.Category.playback, options: avopts)
                try AVAudioSession.sharedInstance().setActive(true)

                try audioPlayer = AVAudioPlayer(contentsOf: url!, fileTypeHint: AVFileType.wav.rawValue)

                if(audioPlayer == nil) {
                    print("player is nil")
                    self.flutterResult!(false)
                }
                
                audioPlayer!.setVolume(1.0, fadeDuration:0)
                audioPlayer!.delegate = self

                audioPlayer!.play()
            } catch {
                print("start error: \(error)")
                self.flutterResult!(false)
            }
        } else {
            print("url null")
            self.flutterResult!(false)
        }
        
    }
    
    public func stop(result:FlutterResult){
        
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
            self.flutterResult!(true)
            result(true)
        } catch {
            print("AVAudioSession stop error: \(error)")
            self.flutterResult!(false)
            result(false)
        }
	
	audioPlayer?.stop()
        
    }
    
    public func pause(result:FlutterResult){
        audioPlayer?.pause()
        
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
            result(true)
        } catch {
            print("AVAudioSession pause error: \(error)")
            result(false)
        }
        
    }
    
    public func resume(result:FlutterResult){
        
        do {
            try AVAudioSession.sharedInstance().setActive(true)
            result(true)
        } catch {
            print("AVAudioSession resume error: \(error)")
            result(false)
        }
	audioPlayer?.play()
        
    }
    
    public func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        do {
            try AVAudioSession.sharedInstance().setActive(false,options: .notifyOthersOnDeactivation)
            self.flutterResult!(true)
        } catch {
            print("AVAudioSession audioPlayerDidFinishPlaying error: \(error)")
            self.flutterResult!(false)
        }
    }
}
