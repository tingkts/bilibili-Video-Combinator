
@echo off

for /l %%x in (1, 1, 30) do (
   echo %%x
   "D:\Program Portable\ffmpeg-20200824-3477feb-win64-static\bin\ffmpeg.exe" -i %%x\64\video.m4s -i %%x\64\audio.m4s -c copy %%x.mp4
)