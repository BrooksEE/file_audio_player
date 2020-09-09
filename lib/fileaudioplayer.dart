import 'dart:async';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/services.dart';

class FileAudioPlayer {
  static const MethodChannel _channel = const MethodChannel('fileaudioplayer');

  FileAudioPlayer();

  Map<String, File> _assetCache = Map<String, File>();

  loadAssets(List<String> fileNames) {
    fileNames.forEach((element) async {
      File f = await _fetchToMemory(element);
      _assetCache[element] = f;
    });
  }

  cleanUp() {
    _assetCache.keys.forEach((element) async {
      _assetCache[element].delete();
    });
    _assetCache.clear();
  }  

  Future<File> _fetchToMemory(String fileName) async {
    final file = File('${(await getTemporaryDirectory()).path}/$fileName');
    await file.create(recursive: true);
    return await file
        .writeAsBytes((await _fetchAsset(fileName)).buffer.asUint8List());
  }

  Future<ByteData> _fetchAsset(String fileName) async {
    return await rootBundle.load(fileName);
  }

  Future<void> playAsset(String asset) async {
    try {
      await start(_assetCache[asset].path);
    } on PlatformException catch (e) {
      print("Stream start error : $e");
    }
  }

  Future<void> start(String path, [String channel='default']) async {
    try {
      await _channel.invokeMethod("start", {"url":path, "channel":channel});
    } on PlatformException catch (e) {
      print("Stream start error : $e");
    }
  }

  Future<void> stop([String channel='default']) async {
    try {
      await _channel.invokeMethod("stop", {"channel":channel});
    } on PlatformException catch (e) {
      print("Stream stop error : $e");
    }
  }

  Future<void> pause([String channel='default']) async {
    try {
      await _channel.invokeMethod("pause", {"channel":channel});
    } on PlatformException catch (e) {
      print("Stream pause error : $e");
    }
  }

  Future<void> resume([String channel='default']) async {
    try {
      await _channel.invokeMethod("resume", {"channel":channel});
    } on PlatformException catch (e) {
      print("Stream resume error : $e");
    }
  }
}
