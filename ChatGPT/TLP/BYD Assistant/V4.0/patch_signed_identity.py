#!/usr/bin/env python3
"""Stage three after dynamic + Bluetooth patches: independent app id, stable signing.

Keystore bytes and password must come only from user-controlled GitHub Actions
secrets. NEVER commit either secret to this public repository.
"""
from pathlib import Path
import sys

root=Path(sys.argv[1]).resolve()
p=root/"app/build.gradle.kts"
s=p.read_text(encoding="utf-8")

old_id='applicationId = "dev.vehiclebridge.byd"'
new_id='applicationId = "com.tlp1998.xiaodi"'
if s.count(old_id)!=1:
    raise RuntimeError("Unexpected upstream app id or already patched")
s=s.replace(old_id,new_id)

old_version='versionCode = 40; versionName = "4.0.0-dev"'
new_version='versionCode = 400; versionName = "4.0.0"'
if s.count(old_version)!=1:
    raise RuntimeError("Expected previously applied V4 source patch")
s=s.replace(old_version,new_version)

old=' buildTypes { getByName("release") { isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }'
new=''' signingConfigs {
     create("xiaodiV4") {
       val keystorePath=System.getenv("XIAODI_KEYSTORE_PATH")
           ?: error("Missing XIAODI_KEYSTORE_PATH")
       val signingPassword=System.getenv("XIAODI_KEYSTORE_PASSWORD")
           ?: error("Missing XIAODI_KEYSTORE_PASSWORD")
       storeFile=file(keystorePath)
       storePassword=signingPassword
       keyAlias="xiaodi_v4"
       keyPassword=signingPassword
     }
 }
 buildTypes {
   getByName("release") {
     isMinifyEnabled=false
     signingConfig=signingConfigs.getByName("xiaodiV4")
   }
 }'''
if s.count(old)!=1:
    raise RuntimeError("Unexpected upstream release signing configuration")
s=s.replace(old,new)
p.write_text(s,encoding="utf-8")
print("Prepared independent com.tlp1998.xiaodi 4.0.0 (400) with stable release signing.")
