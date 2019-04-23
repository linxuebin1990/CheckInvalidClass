package com.res.check

import com.android.builder.model.MavenCoordinates

public class Utils {

  static final String TAG = "Utils"
  static final String LOG_FINE_NAME = "checkInvalidClass.txt"
  static String LOG_PATH = "./" + LOG_FINE_NAME
  static String FILE_SPLIT_STR = File.separator.equals("/") ? File.separator : "\\\\"
  static int PATH_INDEX = File.separator.equals("/") ? 0 : 1
  public static MavenCoordinates parseMavenString(String component) {
    String[] arrays = component.split(":")
    return new MavenCoordinates() {
      @Override
      String getGroupId() {
        return arrays[0]
      }

      @Override
      String getArtifactId() {
        return arrays[1]
      }

      @Override
      String getVersion() {
        return arrays[2]
      }

      @Override
      String getPackaging() {
        return null
      }

      @Override
      String getClassifier() {
        return null
      }

      String getVersionlessId() {
        return null
      }
    }
  }


  public static Map<String, String> sortMapping(Map<String, String> map) {
    List<Map.Entry<String, String>> list = new LinkedList<>(map.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
      public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
        return o2.key.length() - o1.key.length()
      }
    });

    Map<String, String> result = new LinkedHashMap<>();
    for (Iterator<Map.Entry<String, String>> it = list.iterator(); it.hasNext();) {
      Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
      result.put(entry.getKey(), entry.getValue());
    }

    return result;
  }
    /**
     * aapt_rule sample:
     * # view res/layout/abc_alert_dialog_button_bar_material.xml #generated:43
     * # view res/layout/abc_alert_dialog_material.xml #generated:52
     * # view res/layout/abc_alert_dialog_material.xml #generated:66
     * # view res/layout/abc_alert_dialog_title_material.xml #generated:56
     * -keep class android.support.v4.widget.Space { <init>(...); }
     *
     * # Referenced at /Users/xxx/MyProject/app-project/app/src/main/res/layout/activity_filter_rec_detail.xml:69
     * # Referenced at /Users/xxx/MyProject/app-project/app/src/main/res/layout/activity_color_filter_rec_detail.xml:67
     * -keep class cn.xxx.PhotoFilterViewIndicator { <init>(...); }
     * @param rulesPath
     * @return
     */
  public static Map<String, List<String>> parseAaptRulesFindInvalidClass(String rulesPath, Map mappingMap, List<String> whiteList) {
      File aaptRules = new File(rulesPath)
      List<String> tmpXmlPath = new LinkedList<String>()
      Map<String, Set<String>> resultMap = new HashMap<String, Set<String>>()
      log TAG, ""
      log TAG, "start parseAaptRulesFindInvalidClass"
      for (String line : aaptRules.readLines()) {
          if (line.startsWith("# view")) {
              line = line.replace("# view ", '')
              if (line.split(' ')[0].equals("AndroidManifest.xml")) {
                  tmpXmlPath.add("AndroidManifest.xml")
              } else {
                  String keyPath = line.split(' ')[0]
                  if (!tmpXmlPath.contains(keyPath)) {
                      tmpXmlPath.add(keyPath)
                  }
              }
          } else if (line.startsWith("# Referenced ")) { // aapt2
              line = line.replace("# Referenced at ", '')
              if (line.contains("AndroidManifest.xml")) {
                  tmpXmlPath.add("AndroidManifest.xml")
              } else {
                  String keyPath = line.split(":")[PATH_INDEX]
                  if (!tmpXmlPath.contains(keyPath)) {
                      tmpXmlPath.add(keyPath)
                  }
              }
          } else if (line.startsWith("-keep class ")) {
              line = line.replace("-keep class ", '')
              String[] strings = line.split(" ")

              if (strings.length > 1) {
                  String className = strings[0]
                  if (className == null || className.isEmpty()) {
                      continue
                  }
                  boolean isInWhiteList = false
                  for (String str : whiteList) {
                      if (str != null && str.length() > 0) {
                          if (className.startsWith(str)) {
                              isInWhiteList = true
                              break
                          }
                      }
                  }
                  if (isInWhiteList) {
                      continue
                  }
                  String value = mappingMap.get(className)
                  if (value == null || value.isEmpty()) {
                      for (String path : tmpXmlPath) {
                          if (resultMap.containsKey(path)) {
                              ((Set) resultMap.get(path)).add(className)
                          } else {
                              Set<String> subSet = new HashSet<>()
                              subSet.add(className)
                              resultMap.put(path, subSet)
                          }
                      }
                  }
                  tmpXmlPath.clear()
              }
          }
      }
      log TAG, "end parseAaptRulesFindInvalidClass"
      log TAG, ""
      return resultMap
  }
    /**
     * # Referenced at /Users/xxx/MyProject/app-project/app/src/main/res/layout/activity_filter_rec_detail.xml:69
     * # Referenced at /Users/xx/MyProject/app-project/app/src/main/res/layout/activity_color_filter_rec_detail.xml:67
     * -keep class cn.xxx.PhotoFilterViewIndicator { <init>(...); }* @param rulesPath
     * @return
     */
    public static List<String> parseAaptRulesGetXml(String rulesPath) {
        File aaptRules = new File(rulesPath)
        List<String> tmpXmlPath = new LinkedList<String>()
        for (String line : aaptRules.readLines()) {
            if (line.startsWith("# Referenced ") && !line.contains("AndroidManifest.xml")) {
                // aapt2
                String pathStr = line.replace("# Referenced at ", "").split(":")[PATH_INDEX]
                if (pathStr != null && !pathStr.equals("") && !tmpXmlPath.contains(pathStr)) {
                    tmpXmlPath.add(pathStr)
                }
            }
        }
        List<String> resultPath = new LinkedList<>()
        resultPath.addAll(tmpXmlPath)
        // check v21 an so on
        for (String str : tmpXmlPath) {
            File file  = new File(str)
            if (file.exists()
                    && file.getParentFile().exists()
                    && !file.getParentFile().name.contains("-")
                    && file.getParentFile().getParentFile().exists()) {
                File pp = file.getParentFile().getParentFile()
                FilenameFilter filenameFilter = new FilenameFilter() {
                    @Override
                    boolean accept(File filterFile, String name) {
                        return name.contains("-")
                    }
                }
                List<String> list = pp.list(filenameFilter)
                for (String lp : list) {
                    File tt = new File(pp.getPath() + File.separator + lp + File.separator + file.name)
                    if (tt.exists() && !resultPath.contains(tt.path)) {
                        resultPath.add(tt.path)
                        log TAG, "add versionRes: " + tt.path
                    }
                }
            }
        }
        log TAG, ""
        return resultPath
    }

    public static List<String> parseWhiteList(String whiteListPath) {
        log TAG, "whiteListPath = " + whiteListPath
        File whiteListFile = new File(whiteListPath)
        List<String> whiteList = new LinkedList<String>()
        if (!whiteListFile.exists()) {
            return whiteList
        }
        for (String line : whiteListFile.readLines()) {
            if (line != null && line.length() != 0 && !whiteList.contains(line)) {
                log TAG, "whiteList add " + line
                whiteList.add(line)
            }
        }
        Utils.log TAG, ""
        return whiteList
    }

    public static void log(String tag, String msg, boolean needWrite) {
        String logMsg = String.format("[checkInvaliClass] %s: %s", tag, msg)
        println logMsg
        if (needWrite) {
            File logFile = new File(LOG_PATH)
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            logFile.append(logMsg + "\n")
        }
    }

    public static void log(String tag, String msg) {
        log(tag, msg, true)
    }

}
