#!/bin/sh
set -e

UPLOAD_DIR="/data/uploads"
MARKER="$UPLOAD_DIR/.templates_initialized"

# 首次启动时将 JAR 内的模板文件 (Excel 等) 复制到持久化卷
# 只复制 *.xls 模板, 不覆盖用户上传的文件
if [ ! -f "$MARKER" ]; then
    echo "[entrypoint] 初始化上传目录模板文件..."
    mkdir -p "$UPLOAD_DIR"
    TMPDIR=$(mktemp -d)
    # 提取 JAR 中 static/upload/ 下的模板文件
    unzip -o -q /app/app.jar "BOOT-INF/classes/static/upload/*.xls" -d "$TMPDIR" 2>/dev/null || true
    if [ -d "$TMPDIR/BOOT-INF/classes/static/upload" ]; then
        cp -n "$TMPDIR/BOOT-INF/classes/static/upload/"*.xls "$UPLOAD_DIR/" 2>/dev/null || true
    fi
    rm -rf "$TMPDIR"
    touch "$MARKER"
    echo "[entrypoint] 模板文件初始化完成"
fi

exec "$@"
