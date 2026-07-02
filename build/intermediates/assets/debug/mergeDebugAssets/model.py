"""
TensorFlow Lite Model Analyzer
Works with:
    classifier_224.tflite
    classifier_labelmap
    detect.tflite
    detect_labelmap

Requirements:
    pip install tensorflow
"""

import os
import json
import tensorflow as tf

# ============================================================
# FILES
# ============================================================

MODELS = {
    "classifier": {
        "model": "classifier_224.tflite",
        "labels": "classifier_labelmap"
    },
    "detect": {
        "model": "detect.tflite",
        "labels": "detect_labelmap"
    }
}

OUTPUT_JSON = "models_info.json"

# ============================================================
# ANALYSIS FUNCTION
# ============================================================

def analyze_tflite_model(model_path, label_path):
    try:
        interpreter = tf.lite.Interpreter(model_path=model_path)
        interpreter.allocate_tensors()

        input_details = interpreter.get_input_details()[0]
        output_details = interpreter.get_output_details()[0]

        # Input
        input_shape = input_details["shape"].tolist()
        input_dtype = str(input_details["dtype"])

        # Output
        output_shape = output_details["shape"].tolist()
        output_dtype = str(output_details["dtype"])

        # Quantization
        quantization = {}

        q_in = input_details.get("quantization", (0, 0))
        q_out = output_details.get("quantization", (0, 0))

        if q_in[0] != 0 or q_in[1] != 0:
            quantization["input_scale"] = float(q_in[0])
            quantization["input_zero_point"] = int(q_in[1])

        if q_out[0] != 0 or q_out[1] != 0:
            quantization["output_scale"] = float(q_out[0])
            quantization["output_zero_point"] = int(q_out[1])

        if "int8" in input_dtype or "uint8" in input_dtype:
            quantization["is_quantized"] = True
        else:
            quantization["is_quantized"] = False

        # Operators
        try:
            ops = interpreter._get_ops_details()
            operators_used = sorted(set(op["op_name"] for op in ops))
        except Exception:
            operators_used = ["Unable to extract operators"]

        # Model size
        model_size_mb = round(os.path.getsize(model_path) / (1024 * 1024), 2)

        # Labels
        labels = []

        if os.path.exists(label_path):
            with open(label_path, "r", encoding="utf-8") as f:
                labels = [line.strip() for line in f if line.strip()]

        return {
            "model_path": model_path,
            "label_path": label_path,
            "input_shape": input_shape,
            "input_dtype": input_dtype,
            "output_shape": output_shape,
            "output_dtype": output_dtype,
            "quantization": quantization,
            "operators_used": operators_used,
            "model_size_mb": model_size_mb,
            "label_count": len(labels),
            "labels": labels
        }

    except Exception as e:
        print(f"ERROR analyzing {model_path}")
        print(e)
        return None

# ============================================================
# MAIN
# ============================================================

def main():

    all_models_info = {}

    print("=" * 60)
    print("TFLITE MODEL ANALYSIS")
    print("=" * 60)

    for name, files in MODELS.items():

        model_file = files["model"]
        label_file = files["labels"]

        print(f"\nAnalyzing: {model_file}")

        result = analyze_tflite_model(model_file, label_file)

        if result:
            all_models_info[name] = result

            print("\nINPUT:")
            print("Shape :", result["input_shape"])
            print("Dtype :", result["input_dtype"])

            print("\nOUTPUT:")
            print("Shape :", result["output_shape"])
            print("Dtype :", result["output_dtype"])

            print("\nQUANTIZATION:")
            print(result["quantization"])

            print("\nOPERATORS:")
            for op in result["operators_used"]:
                print("-", op)

            print("\nMODEL SIZE:")
            print(result["model_size_mb"], "MB")

            print("\nLABEL COUNT:")
            print(result["label_count"])

    # Save JSON
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(all_models_info, f, indent=4)

    print("\n" + "=" * 60)
    print(f"Saved to {OUTPUT_JSON}")
    print("=" * 60)

if __name__ == "__main__":
    main()