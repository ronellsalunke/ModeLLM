# Glance instantiates action callbacks through their zero-argument constructors.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { public <init>(); }
