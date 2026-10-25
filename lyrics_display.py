from flask import Flask, jsonify, request


app = Flask(__name__)
latest_message = None


@app.post("/now-playing")
def now_playing():
    global latest_message
    message = request.get_json(silent=True)

    if not isinstance(message, dict):
        return jsonify({"error": "Expected a JSON object"}), 400

    if "title" not in message or not isinstance(message["title"], str):
        return jsonify({"error": "Expected 'title' to be a string"}), 400

    if "artist" not in message or not isinstance(message["artist"], str):
        return jsonify({"error": "Expected 'artist' to be a string"}), 400

    if (
        "position_ms" not in message
        or not isinstance(message["position_ms"], int)
        or isinstance(message["position_ms"], bool)
        or message["position_ms"] < 0):

        return jsonify({"error": "Expected 'position_ms' to be a nonnegative integer"}), 400

    if "is_playing" not in message or not isinstance(message["is_playing"], bool):
        return jsonify({"error": "Expected 'is_playing' to be a boolean"}), 400

    print("Received:", message)
    latest_message = message
    return jsonify({"ok": True}), 200


@app.get("/now-playing")
def get_now_playing():
    return jsonify({"message": latest_message})


@app.errorhandler(404)
def not_found(_error):
    return jsonify({"error": "Route not found"}), 404


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000, debug=False)
