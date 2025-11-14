import { useEffect, useState } from "react";

const api = import.meta.env.VITE_API_URL ?? "";

export default function App() {
  const [msg, setMsg] = useState<string>("…");

  useEffect(() => {
    fetch(`${api}/api/hello`)
      .then((r) => r.json())
      .then((d: { message: string }) => setMsg(d.message))
      .catch((e) => setMsg(String(e)));
  }, []);

  return (
    <main
      style={{
        fontFamily: "system-ui",
        padding: 24,
        maxWidth: 720,
        margin: "8vmin auto",
      }}
    >
      <h1>React + TypeScript → Spring</h1>
      <p>API base: {api || "(not set)"}</p>
      <p>Message: {msg}</p>
    </main>
  );
}
