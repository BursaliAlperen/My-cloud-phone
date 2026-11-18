const express = require("express");
const multer = require("multer");
const crypto = require("crypto");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { execFile } = require("child_process");
const { promisify } = require("util");

const execFileAsync = promisify(execFile);
const app = express();
const port = Number(process.env.PORT || 8080);
const token = process.env.MYCLOUDPHONE_UPLOAD_TOKEN || "";
const serial = process.env.ADB_SERIAL || "";
const uploadDir = fs.mkdtempSync(path.join(os.tmpdir(), "mycloudphone-"));

if (token.length < 24) {
  console.error("MYCLOUDPHONE_UPLOAD_TOKEN must be set to a random value of at least 24 characters.");
  process.exit(1);
}

const upload = multer({
  dest: uploadDir,
  limits: { fileSize: 250 * 1024 * 1024, files: 1 },
  fileFilter: (_req, file, cb) => {
    const ok = file.mimetype === "application/vnd.android.package-archive" ||
      file.originalname.toLowerCase().endsWith(".apk");
    cb(ok ? null : new Error("Only APK files are accepted."), ok);
  }
});

function authorized(req) {
  const supplied = (req.get("authorization") || "").replace(/^Bearer\s+/i, "");
  const a = Buffer.from(supplied);
  const b = Buffer.from(token);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

app.get("/api/health", (_req, res) => res.json({ ok: true, service: "MyCloudPhone install API" }));

app.post("/api/apps/install", (req, res) => {
  if (!authorized(req)) return res.status(401).json({ error: "Unauthorized" });
  upload.single("apk")(req, res, async (err) => {
    if (err) return res.status(400).json({ error: err.message });
    if (!req.file) return res.status(400).json({ error: "Missing apk file field." });

    const file = req.file.path;
    try {
      const fd = fs.openSync(file, "r");
      const header = Buffer.alloc(4);
      fs.readSync(fd, header, 0, 4, 0);
      fs.closeSync(fd);
      if (header.toString("hex") !== "504b0304") {
        return res.status(400).json({ error: "Uploaded file is not a valid APK/ZIP archive." });
      }

      const args = [];
      if (serial) args.push("-s", serial);
      args.push("install", "-r", file);
      const { stdout, stderr } = await execFileAsync("adb", args, { timeout: 180000, maxBuffer: 1024 * 1024 });
      const output = (stdout + "\n" + stderr).trim();
      if (!/Success/i.test(output)) {
        return res.status(502).json({ error: "ADB installation failed.", details: output.slice(-1200) });
      }
      return res.json({ ok: true, installed: true, output });
    } catch (e) {
      return res.status(502).json({ error: "Could not install APK on the configured Android VM.", details: String(e.message).slice(0, 1000) });
    } finally {
      fs.promises.unlink(file).catch(() => {});
    }
  });
});

app.use((err, _req, res, _next) => {
  res.status(400).json({ error: err.message || "Bad request" });
});

async function start() {
  if (serial && serial.includes(":")) {
    try {
      const { stdout, stderr } = await execFileAsync("adb", ["connect", serial], { timeout: 15000 });
      console.log((stdout + " " + stderr).trim());
    } catch (e) {
      console.error("ADB connect failed:", e.message);
      process.exit(1);
    }
  }
  app.listen(port, "0.0.0.0", () => {
    console.log("MyCloudPhone install API listening on " + port);
  });
}
start();
