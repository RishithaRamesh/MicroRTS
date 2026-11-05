#!/usr/bin/env python3
"""Create reproducible D2 distribution plots and descriptive summaries."""

import argparse
import csv
import os
import statistics
import tempfile
from collections import defaultdict
from pathlib import Path

os.environ.setdefault(
    "MPLCONFIGDIR", str(Path(tempfile.gettempdir()) / "factory-feud-mpl-cache")
)
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


METRICS = (
    ("actual_density", "Actual obstacle density"),
    ("mean_path_length", "Mean factory-to-cocoa path length"),
    ("route_imbalance", "Route imbalance"),
    ("dead_end_count", "Dead-end count"),
)
FAMILY_ORDER = ("OPEN", "OBSTACLE_FIELD", "CHOKEPOINT")
PRESET_ORDER = ("SPARSE", "LOW", "MEDIUM", "HIGH")


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    return parser.parse_args()


def load_rows(path):
    with path.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        raise ValueError(f"No accepted-map rows found in {path}")
    for row in rows:
        row["actual_density"] = float(row["actual_density"])
        row["player0_path_length"] = int(row["player0_path_length"])
        row["player1_path_length"] = int(row["player1_path_length"])
        row["mean_path_length"] = (
            row["player0_path_length"] + row["player1_path_length"]
        ) / 2.0
        row["route_imbalance"] = int(row["route_imbalance"])
        row["dead_end_count"] = int(row["dead_end_count"])
    return rows


def describe(values):
    return {
        "count": len(values),
        "mean": statistics.fmean(values),
        "stddev": statistics.stdev(values) if len(values) > 1 else 0.0,
        "minimum": min(values),
        "median": statistics.median(values),
        "maximum": max(values),
    }


def grouped(rows, key):
    groups = defaultdict(list)
    for row in rows:
        groups[key(row)].append(row)
    return groups


def write_statistics(rows, output):
    groups = [("OVERALL", "OVERALL", rows)]
    by_family = grouped(rows, lambda row: row["family"])
    for family in FAMILY_ORDER:
        groups.append(("FAMILY", family, by_family[family]))
    by_family_preset = grouped(
        rows, lambda row: (row["family"], row["density_preset"])
    )
    for family in FAMILY_ORDER:
        for preset in PRESET_ORDER:
            selected = by_family_preset.get((family, preset), [])
            if selected:
                groups.append(("FAMILY_PRESET", f"{family}:{preset}", selected))

    header = [
        "group_type",
        "group",
        "metric",
        "count",
        "mean",
        "stddev",
        "minimum",
        "median",
        "maximum",
    ]
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(header)
        for group_type, name, selected in groups:
            for metric, _ in METRICS:
                stats = describe([row[metric] for row in selected])
                writer.writerow(
                    [
                        group_type,
                        name,
                        metric,
                        stats["count"],
                        f'{stats["mean"]:.8f}',
                        f'{stats["stddev"]:.8f}',
                        f'{stats["minimum"]:.8f}',
                        f'{stats["median"]:.8f}',
                        f'{stats["maximum"]:.8f}',
                    ]
                )


def plot_metric(rows, metric, label, output):
    by_family = grouped(rows, lambda row: row["family"])
    by_family_preset = grouped(
        rows, lambda row: (row["family"], row["density_preset"])
    )
    fig, axes = plt.subplots(1, 2, figsize=(14, 5.5))
    family_values = [[row[metric] for row in by_family[name]] for name in FAMILY_ORDER]
    axes[0].boxplot(family_values, tick_labels=FAMILY_ORDER, showmeans=True)
    axes[0].set_title(f"{label} by map family")
    axes[0].set_ylabel(label)
    axes[0].grid(axis="y", alpha=0.25)

    labels = []
    values = []
    for family in FAMILY_ORDER:
        for preset in PRESET_ORDER:
            selected = by_family_preset.get((family, preset), [])
            if selected:
                labels.append(f"{family}\n{preset}")
                values.append([row[metric] for row in selected])
    axes[1].boxplot(values, tick_labels=labels, showmeans=True)
    axes[1].set_title(f"{label} by family and density preset")
    axes[1].tick_params(axis="x", labelrotation=55, labelsize=8)
    axes[1].grid(axis="y", alpha=0.25)
    fig.tight_layout()
    fig.savefig(output, dpi=160, metadata={"Software": "Factory Feud D2 analysis"})
    plt.close(fig)


def write_observations(rows, output):
    by_family = grouped(rows, lambda row: row["family"])
    lines = [
        "# Factory Feud generated-map observations",
        "",
        "These statements are descriptive summaries of the generated batch; they do not claim statistical significance or causal effects.",
        "",
    ]
    for metric, label in METRICS:
        means = {
            family: statistics.fmean(row[metric] for row in by_family[family])
            for family in FAMILY_ORDER
        }
        highest = max(means, key=means.get)
        lowest = min(means, key=means.get)
        lines.append(
            f"- {label}: highest family mean was `{highest}` ({means[highest]:.3f}); "
            f"lowest was `{lowest}` ({means[lowest]:.3f})."
        )
    lines.extend(
        [
            "",
            "The family-and-preset panels retain all individual observations through boxplot distributions. Differences should be interpreted alongside the requested densities because preset numeric values are family-specific.",
            "",
        ]
    )
    output.write_text("\n".join(lines), encoding="utf-8")


def main():
    args = parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    rows = load_rows(args.input)
    write_statistics(rows, args.output_dir / "factory-feud-property-summary.csv")
    write_observations(rows, args.output_dir / "factory-feud-observations.md")
    for metric, label in METRICS:
        plot_metric(
            rows,
            metric,
            label,
            args.output_dir / f"factory-feud-{metric}-distribution.png",
        )
    print(f"rows={len(rows)}")
    print(f"plots={len(METRICS)}")
    print(f"outputDir={args.output_dir.resolve()}")


if __name__ == "__main__":
    main()
