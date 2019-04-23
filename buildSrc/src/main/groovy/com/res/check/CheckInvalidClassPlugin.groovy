package com.res.check

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariantOutput
import org.gradle.api.Plugin
import org.gradle.api.Project

class CheckInvalidClassPlugin implements Plugin<Project> {

    static final String TAG = "CheckInvalidClassPlugin"
    @Override
    void apply(Project project) {

        CheckInvalidClassExtension ext = project.extensions.create("checkInvalidClass", CheckInvalidClassExtension.class)
        boolean hasCleanTaskExecute = false;
        project.afterEvaluate {
            project.plugins.withId('com.android.application') {
                project.android.applicationVariants.all { ApplicationVariant variant ->

                    variant.outputs.each { BaseVariantOutput output ->

                        Utils.LOG_PATH = "${project.buildDir.absolutePath}/outputs/logs/${Utils.LOG_FINE_NAME}"
                        File checkInValidClassFile = new File(Utils.LOG_PATH)
                        if (checkInValidClassFile.exists()) {
                            checkInValidClassFile.delete()
                        }
                        String cleanTaskName = "clean"
                        def cleanTask = project.tasks.findByName(cleanTaskName)
                        if (!cleanTask) {
                            hasCleanTask = false;
                            Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                            Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                            Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                            Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                            return
                        } else {
                            cleanTask.doLast {
                                hasCleanTaskExecute = true
                            }
                        }

                        String taskName = "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}"
                        def proguardTask = project.tasks.findByName(taskName)
                        if (!proguardTask) {
                            return
                        }
                        proguardTask.outputs.upToDateWhen {false}

                        boolean hasProguardExecuted = false

                        boolean hasProcessResourcesExecuted = false
                        output.processResources.doLast {
                            Utils.log TAG, "processResources is call"
                            if (hasProcessResourcesExecuted) {
                                return
                            }
                            hasProcessResourcesExecuted = true

                            def rulesPath = "${project.buildDir.absolutePath}/intermediates/proguard-rules/${variant.dirName}/aapt_rules.txt"
                            def rulesPathCopy = "${project.buildDir.absolutePath}/intermediates/proguard-rules/${variant.dirName}/aapt_rules_copy_for_check.txt"
                            File aaptRules = new File(rulesPath)
                            File aaptRulesCopy = new File(rulesPathCopy)
                            aaptRulesCopy << aaptRules.text
                            Utils.log TAG, ""
                            Utils.log TAG, "keep aaptRules text = " + aaptRulesCopy.text
                            Utils.log TAG, ""
                        }

                        proguardTask.doLast {
                            hasProguardExecuted = true
                            CheckInvalidClassComponentTask checkTask = project.tasks.create(name: "checkInValidClassComponentFor${variant.name.capitalize()}",
                                    type: CheckInvalidClassComponentTask
                            ) {
                                applicationVariant = variant
                                whiteList = ext.whiteList
                                variantOutput = output
                            }
                            if (hasCleanTaskExecute) {
                                Utils.log TAG, "check invalid class task finish, ready to check in valid class in AndroidManifest or layout"
                                checkTask.execute()
                            } else {
                                Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                                Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                                Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                                Utils.log TAG, "Warning: can not find clean task, check invalid class can not be execute!"
                            }
                        }

                    }
                }
            }
        }
    }
}
