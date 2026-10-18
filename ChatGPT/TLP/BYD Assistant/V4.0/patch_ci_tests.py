#!/usr/bin/env python3
"""Align original upstream unit test fixture with V4's confirmed-lock safety rule.

This adjusts only a test double, not any real vehicle request. An ACCEPTED
request is not the same as confirmed SUCCESS: V4 sends CLOSEAIR after confirmed
LOCKDOOR success, and skips it when the lock outcome is still unresolved.
"""
from pathlib import Path
import sys

root=Path(sys.argv[1])
path=root/"app/src/test/java/dev/vehiclebridge/byd/data/ShutdownControlTest.kt"
s=path.read_text(encoding="utf-8")
pairs=[
    ('var outcome=RemoteOutcome.ACCEPTED','var outcome=RemoteOutcome.SUCCESS'),
    ('assertTrue(result.controlMessage!!.contains("锁车：请求已发送") && result.controlMessage!!.contains("熄火：请求已发送"))',
     'assertTrue(result.controlMessage!!.contains("锁车：服务器报告执行成功") && result.controlMessage!!.contains("熄火：服务器报告执行成功"))'),
    ('assertTrue(error.label.contains("锁车：请求已发送"))',
     'assertTrue(error.label.contains("锁车：服务器报告执行成功"))')
]
for old,new in pairs:
    if s.count(old)!=1:
        if new in s: continue
        raise RuntimeError("Unexpected upstream test fixture: "+old[:100])
    s=s.replace(old,new)
path.write_text(s,encoding="utf-8")
print("Updated historical shutdown unit tests to represent confirmed remote lock success.")
