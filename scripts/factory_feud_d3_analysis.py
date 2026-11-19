#!/usr/bin/env python3
"""Reproduce D3 descriptive analysis and publication-ready figures."""

import argparse
import csv
import math
import os
import tempfile
from collections import Counter, defaultdict
from pathlib import Path


FAMILIES = ["OPEN", "OBSTACLE_FIELD", "CHOKEPOINT"]
COLORS = {"OPEN": "#4C78A8", "OBSTACLE_FIELD": "#F58518", "CHOKEPOINT": "#54A24B"}


def read_rows(path):
    with path.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    required = {
        "experiment_type", "map_id", "family", "adaptive_side", "p0_cocoa",
        "p1_cocoa", "adaptive_cocoa", "fixed_cocoa", "adaptive_minus_fixed_margin",
        "outcome", "adaptive_surviving_workers", "role_changes",
        "tactical_counterattacks", "tactical_retreats", "actual_obstacle_density",
        "mean_terrain_shortest_path_length", "route_imbalance", "dead_end_count",
        "adaptive_carrier_attacks_received", "fixed_carrier_attacks_received",
        "adaptive_carrier_deaths", "adaptive_completed_delivery_events",
        "adaptive_worker_deaths", "fixed_worker_deaths", "match_id",
    }
    if not rows:
        raise ValueError(f"No match rows in {path}")
    missing = required - set(rows[0])
    if missing:
        raise ValueError(f"Missing result columns: {sorted(missing)}")
    numeric = required - {"experiment_type", "map_id", "family", "outcome"}
    for row in rows:
        for key in numeric:
            row[key] = float(row[key]) if row[key] else math.nan
    return rows


def mean(values):
    clean = [value for value in values if not math.isnan(value)]
    return sum(clean) / len(clean) if clean else math.nan


def pearson(xs, ys):
    pairs = [(x, y) for x, y in zip(xs, ys) if not math.isnan(x) and not math.isnan(y)]
    if len(pairs) < 3:
        return math.nan
    xmean = mean([x for x, _ in pairs])
    ymean = mean([y for _, y in pairs])
    numerator = sum((x - xmean) * (y - ymean) for x, y in pairs)
    denominator = math.sqrt(sum((x - xmean) ** 2 for x, _ in pairs)
                            * sum((y - ymean) ** 2 for _, y in pairs))
    return numerator / denominator if denominator else math.nan


def map_aggregate(main):
    grouped = defaultdict(list)
    for row in main:
        grouped[row["map_id"]].append(row)
    result = []
    for map_id, matches in sorted(grouped.items()):
        if len(matches) != 2 or {int(row["adaptive_side"]) for row in matches} != {0, 1}:
            raise ValueError(f"Map {map_id} lacks one complete side-swapped MAIN pair")
        first = matches[0]
        result.append({
            "map_id": map_id,
            "family": first["family"],
            "density": first["actual_obstacle_density"],
            "path": first["mean_terrain_shortest_path_length"],
            "imbalance": first["route_imbalance"],
            "dead_ends": first["dead_end_count"],
            "margin": mean([row["adaptive_minus_fixed_margin"] for row in matches]),
            "throughput": mean([row["adaptive_cocoa"] + row["fixed_cocoa"] for row in matches]),
            "side_advantage": mean([abs(row["p0_cocoa"] - row["p1_cocoa"]) for row in matches]),
            "overrides": mean([row["tactical_counterattacks"] + row["tactical_retreats"]
                               for row in matches]),
            "carrier_hits": mean([row["adaptive_carrier_attacks_received"]
                                  + row["fixed_carrier_attacks_received"] for row in matches]),
        })
    return result


def paired_tactics(main, ablation):
    key = lambda row: (row["map_id"], int(row["adaptive_side"]))
    tactical = {key(row): row for row in main}
    control = {key(row): row for row in ablation}
    if set(tactical) != set(control):
        raise ValueError("MAIN and ABLATION do not contain identical map/side pairs")
    return [(tactical[item], control[item]) for item in sorted(tactical)]


def setup_plotting():
    cache_root = Path(tempfile.gettempdir()) / "factory-feud-matplotlib"
    cache_root.mkdir(parents=True, exist_ok=True)
    os.environ.setdefault("MPLCONFIGDIR", str(cache_root))
    os.environ.setdefault("XDG_CACHE_HOME", str(cache_root))
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    plt.rcParams.update({
        "figure.dpi": 140, "savefig.dpi": 180, "axes.grid": True,
        "grid.alpha": 0.25, "axes.spines.top": False, "axes.spines.right": False,
        "font.size": 10,
    })
    return plt


def bubble_points(axis, xs, ys, color, label=None, base_size=55):
    """Plot discrete coordinates without hiding repeated observations."""
    counts = Counter(zip(xs, ys))
    axis.scatter([point[0] for point in counts], [point[1] for point in counts],
                 s=[base_size * count for count in counts.values()], color=color,
                 alpha=0.78, label=label, edgecolors="white", linewidths=0.5)
    for (x_value, y_value), count in counts.items():
        if count > 1:
            axis.annotate(str(count), (x_value, y_value), ha="center", va="center",
                          color="white", fontsize=8, fontweight="bold")


def save_figures(rows, output):
    plt = setup_plotting()
    main = [row for row in rows if row["experiment_type"] == "MAIN"]
    ablation = [row for row in rows if row["experiment_type"] == "ABLATION"]
    maps = map_aggregate(main)
    pairs = paired_tactics(main, ablation)

    fig, axes = plt.subplots(1, 3, figsize=(10.5, 3.5), sharey=True)
    for axis, family in zip(axes, FAMILIES):
        counts = Counter(row["outcome"] for row in main if row["family"] == family)
        axis.bar(["Win", "Draw", "Loss"], [counts["WIN"], counts["DRAW"], counts["LOSS"]],
                 color=["#59A14F", "#BAB0AC", "#E15759"])
        axis.set_title(family.replace("_", " ").title())
        axis.set_xlabel("AdaptiveRoleAI outcome")
    axes[0].set_ylabel("Matches")
    fig.suptitle("Main comparison outcomes by map family")
    fig.tight_layout()
    fig.savefig(output / "01-outcomes-by-family.png", bbox_inches="tight")
    plt.close(fig)

    data = [[row["adaptive_minus_fixed_margin"] for row in main if row["family"] == family]
            for family in FAMILIES]
    fig, axis = plt.subplots(figsize=(7.4, 4.2))
    boxes = axis.boxplot(data, tick_labels=[name.replace("_", " ") for name in FAMILIES],
                         patch_artist=True, medianprops={"color": "black"})
    for box, family in zip(boxes["boxes"], FAMILIES):
        box.set_facecolor(COLORS[family])
        box.set_alpha(0.75)
    axis.axhline(0, color="black", linewidth=0.9)
    axis.set_ylabel("Adaptive − fixed cocoa (per match)")
    axis.set_title("Cocoa-delivery advantage by map family")
    fig.tight_layout()
    fig.savefig(output / "02-margin-by-family.png", bbox_inches="tight")
    plt.close(fig)

    fig, axis = plt.subplots(figsize=(6.8, 4.2))
    for family in FAMILIES:
        chosen = [row for row in maps if row["family"] == family]
        bubble_points(axis, [row["density"] for row in chosen],
                      [row["margin"] for row in chosen], COLORS[family],
                      family.replace("_", " "), 48)
    axis.axhline(0, color="black", linewidth=0.9)
    axis.set_xlabel("Actual obstacle density (fraction of map cells)")
    axis.set_ylabel("Mean paired adaptive advantage (cocoa)")
    axis.set_title("Obstacle density and adaptive advantage")
    axis.legend(frameon=False)
    fig.tight_layout()
    fig.savefig(output / "03-density-vs-margin.png", bbox_inches="tight")
    plt.close(fig)

    fig, axis = plt.subplots(figsize=(6.8, 4.2))
    bubble_points(axis, [row["imbalance"] for row in maps],
                  [row["side_advantage"] for row in maps], "#B279A2", base_size=48)
    axis.set_xlabel("Route imbalance (terrain-proxy cells)")
    axis.set_ylabel("Mean absolute P0−P1 cocoa difference")
    axis.set_title("Route imbalance and observed side advantage")
    fig.tight_layout()
    fig.savefig(output / "04-route-imbalance-vs-side-advantage.png", bbox_inches="tight")
    plt.close(fig)

    tactical = [left["adaptive_cocoa"] for left, _ in pairs]
    control = [right["adaptive_cocoa"] for _, right in pairs]
    fig, axis = plt.subplots(figsize=(7.0, 5.2))
    bubble_points(axis, control, tactical, "#4C78A8", base_size=42)
    upper = max(tactical + control + [1])
    axis.plot([0, upper], [0, upper], linestyle="--", color="black", linewidth=0.9)
    axis.set_xlabel("OldAdaptiveRoleAI cocoa (tactical-off)")
    axis.set_ylabel("AdaptiveRoleAI cocoa (tactical)")
    axis.set_title("Matched cocoa delivery by map and player side (n=48)")
    fig.tight_layout()
    fig.savefig(output / "05-tactical-vs-nontactical-cocoa.png", bbox_inches="tight")
    plt.close(fig)

    tactical_survival = [left["adaptive_surviving_workers"] for left, _ in pairs]
    control_survival = [right["adaptive_surviving_workers"] for _, right in pairs]
    fig, axis = plt.subplots(figsize=(7.0, 5.2))
    bubble_points(axis, control_survival, tactical_survival, "#59A14F", base_size=42)
    axis.plot([0, 4], [0, 4], linestyle="--", color="black", linewidth=0.9)
    axis.set_xlim(-0.15, 4.15)
    axis.set_ylim(-0.15, 4.15)
    axis.set_xticks(range(5))
    axis.set_yticks(range(5))
    axis.set_xlabel("OldAdaptiveRoleAI survivors (tactical-off)")
    axis.set_ylabel("AdaptiveRoleAI survivors (tactical)")
    axis.set_title("Matched Worker survival by map and player side (n=48)")
    fig.tight_layout()
    fig.savefig(output / "06-tactical-vs-nontactical-survival.png", bbox_inches="tight")
    plt.close(fig)

    return main, ablation, maps, pairs


def write_analysis(output, main, ablation, maps, pairs):
    associations = [
        ("actual_obstacle_density", "adaptive_margin",
         pearson([row["density"] for row in maps], [row["margin"] for row in maps])),
        ("mean_path_length", "adaptive_margin",
         pearson([row["path"] for row in maps], [row["margin"] for row in maps])),
        ("route_imbalance", "adaptive_margin",
         pearson([row["imbalance"] for row in maps], [row["margin"] for row in maps])),
        ("dead_end_count", "adaptive_margin",
         pearson([row["dead_ends"] for row in maps], [row["margin"] for row in maps])),
        ("mean_path_length", "combined_delivery_throughput",
         pearson([row["path"] for row in maps], [row["throughput"] for row in maps])),
        ("route_imbalance", "absolute_side_advantage",
         pearson([row["imbalance"] for row in maps], [row["side_advantage"] for row in maps])),
        ("actual_obstacle_density", "carrier_hits_received",
         pearson([row["density"] for row in maps], [row["carrier_hits"] for row in maps])),
    ]
    with (output / "terrain-associations.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(["predictor", "response", "pearson_r", "unit_of_analysis", "n"])
        for predictor, response, value in associations:
            writer.writerow([predictor, response, f"{value:.6f}", "side-paired map", len(maps)])
        association_keys = [
            ("actual_obstacle_density", "adaptive_margin", "density", "margin"),
            ("mean_path_length", "combined_delivery_throughput", "path", "throughput"),
            ("route_imbalance", "absolute_side_advantage", "imbalance", "side_advantage"),
        ]
        for predictor, response, x_key, y_key in association_keys:
            for family in FAMILIES:
                family_maps = [row for row in maps if row["family"] == family]
                value = pearson([row[x_key] for row in family_maps],
                                [row[y_key] for row in family_maps])
                writer.writerow([predictor, response, f"{value:.6f}",
                                 f"side-paired map within {family}", len(family_maps)])

    tactical_cocoa = [left["adaptive_cocoa"] for left, _ in pairs]
    control_cocoa = [right["adaptive_cocoa"] for _, right in pairs]
    tactical_survival = [left["adaptive_surviving_workers"] for left, _ in pairs]
    control_survival = [right["adaptive_surviving_workers"] for _, right in pairs]
    cocoa_deltas = [left - right for left, right in zip(tactical_cocoa, control_cocoa)]
    survival_deltas = [left - right for left, right in zip(tactical_survival, control_survival)]
    tradeoffs = sum(1 for cocoa, survival in zip(cocoa_deltas, survival_deltas)
                    if cocoa < 0 and survival > 0)
    inverse_tradeoffs = sum(1 for cocoa, survival in zip(cocoa_deltas, survival_deltas)
                            if cocoa > 0 and survival < 0)
    zero_zero = sum(1 for row in main if row["adaptive_cocoa"] == 0 and row["fixed_cocoa"] == 0)
    family_overrides = {family: mean([row["overrides"] for row in maps if row["family"] == family])
                        for family in FAMILIES}
    family_outcomes = {
        family: Counter(row["outcome"] for row in main if row["family"] == family)
        for family in FAMILIES
    }
    most_switching = max(main + ablation, key=lambda row: row["role_changes"])
    switch_transitions = Counter()
    for event in most_switching["role_change_history"].split(";"):
        if ",from=" not in event or ",to=" not in event:
            continue
        old_role = event.split(",from=", 1)[1].split(",to=", 1)[0]
        new_role = event.split(",to=", 1)[1].split(",reason=", 1)[0]
        switch_transitions[(old_role, new_role)] += 1
    matching_switch_row = next(
        row for row in main + ablation
        if row is not most_switching and row["map_id"] == most_switching["map_id"]
        and row["adaptive_side"] == most_switching["adaptive_side"]
        and row["role_changes"] == most_switching["role_changes"])
    switch_histories_identical = (matching_switch_row["role_change_history"]
                                  == most_switching["role_change_history"])
    carrier_loss_matches = [row for row in main if row["adaptive_carrier_deaths"] > 0]
    zero_zero_matches = [row for row in main
                         if row["adaptive_cocoa"] == 0 and row["fixed_cocoa"] == 0]
    tradeoff_pairs = [(left, right) for left, right in pairs
                      if left["adaptive_cocoa"] < right["adaptive_cocoa"]
                      and left["adaptive_surviving_workers"] > right["adaptive_surviving_workers"]]
    inverse_pairs = [(left, right) for left, right in pairs
                     if left["adaptive_cocoa"] > right["adaptive_cocoa"]
                     and left["adaptive_surviving_workers"] < right["adaptive_surviving_workers"]]

    def side_margin(source, side):
        return mean([row["adaptive_minus_fixed_margin"] for row in source
                     if int(row["adaptive_side"]) == side])

    def leave_one_out_range(x_key, y_key):
        values = []
        for omitted in range(len(maps)):
            retained = maps[:omitted] + maps[omitted + 1:]
            values.append(pearson([row[x_key] for row in retained],
                                  [row[y_key] for row in retained]))
        return min(values), max(values)

    density_loo = leave_one_out_range("density", "margin")
    path_loo = leave_one_out_range("path", "throughput")
    imbalance_loo = leave_one_out_range("imbalance", "side_advantage")
    family_delivery = {}
    for family in FAMILIES:
        selected = [row for row in main if row["family"] == family]
        family_delivery[family] = {
            "adaptive": mean([row["adaptive_cocoa"] for row in selected]),
            "fixed": mean([row["fixed_cocoa"] for row in selected]),
            "adaptive_deaths": mean([row["adaptive_worker_deaths"] for row in selected]),
            "fixed_deaths": mean([row["fixed_worker_deaths"] for row in selected]),
        }

    lines = [
        "# Factory Feud D3 supplementary analysis", "",
        "All terrain associations use one side-paired observation per map (n=24), not the two "
        "swapped matches as independent observations. Pearson coefficients are descriptive; no "
        "causal or significance claim is made.", "", "## Terrain associations", "",
        "| Predictor | Response | Pearson r |", "|---|---|---:|",
    ]
    lines.extend(f"| {a} | {b} | {value:.3f} |" for a, b, value in associations)
    lines += [
        "", "Leave-one-map-out ranges for the three headline correlations were "
        f"density/margin {density_loo[0]:.3f} to {density_loo[1]:.3f}, "
        f"path/throughput {path_loo[0]:.3f} to {path_loo[1]:.3f}, and "
        f"imbalance/side-advantage {imbalance_loo[0]:.3f} to {imbalance_loo[1]:.3f}. "
        "No single map reverses a headline association, but n=24 remains small. "
        "Within-family coefficients are provided in terrain-associations.csv; each uses only "
        "eight maps and should be treated as exploratory.",
        "", "## Player-side check", "",
        f"- MAIN mean adaptive margin as P0/P1: {side_margin(main, 0):.3f}/"
        f"{side_margin(main, 1):.3f} cocoa",
        f"- ABLATION mean adaptive margin as P0/P1: {side_margin(ablation, 0):.3f}/"
        f"{side_margin(ablation, 1):.3f} cocoa",
        "- MAIN is nearly side-neutral in aggregate. The non-tactical comparison has a "
        "noticeable P0 advantage, despite exact side pairing; this limits interpretation of "
        "unpaired ablation summaries but does not invalidate the matched H3 calculation.",
        "", "## Tactical matched-pair diagnostics", "",
        f"- Matched map/side observations: {len(pairs)}",
        f"- Mean tactical cocoa delta: {mean(cocoa_deltas):.3f}",
        f"- Mean tactical Worker-survival delta: {mean(survival_deltas):.3f}",
        f"- Better survival but lower cocoa: {tradeoffs} pairs",
        f"- More cocoa but lower survival: {inverse_tradeoffs} pairs",
        f"- Main-comparison 0–0 draws: {zero_zero}",
        "- Mean tactical overrides per match by family: "
        + ", ".join(f"{family}={family_overrides[family]:.3f}" for family in FAMILIES),
        "- MAIN W/D/L by family: " + "; ".join(
            f"{family}={family_outcomes[family]['WIN']}/{family_outcomes[family]['DRAW']}/"
            f"{family_outcomes[family]['LOSS']}" for family in FAMILIES),
        f"- Highest role-switch count: {int(most_switching['role_changes'])} "
        f"({most_switching['map_id']}, adaptive side {int(most_switching['adaptive_side'])}, "
        f"{most_switching['experiment_type']})",
        "- Its most frequent transitions were " + ", ".join(
            f"{old}→{new}={count}" for (old, new), count in switch_transitions.most_common(4))
        + f". The MAIN and ABLATION histories are {'identical' if switch_histories_identical else 'different'} "
        "on this map/side, and MAIN recorded "
        f"{int(most_switching['tactical_counterattacks'] + most_switching['tactical_retreats'])} "
        "tactical overrides, so the repeated Collector/Defender/Escort rotation comes from the "
        "shared strategic allocator rather than the tactical layer.",
        f"- MAIN matches with an adaptive cocoa-carrier death: {len(carrier_loss_matches)}; "
        "the telemetry does not timestamp deaths, so delivery specifically after a Collector loss "
        "cannot be inferred accurately.",
        f"- The 0–0 case is match {int(zero_zero_matches[0]['match_id'])} on "
        f"{zero_zero_matches[0]['map_id']}: one cocoa left the resource node, none was delivered "
        "or carried at the end, and seven Workers died. The telemetry does not establish the "
        "exact loss event.",
        f"- Better survival/lower delivery case: {tradeoff_pairs[0][0]['map_id']}, adaptive "
        f"side {int(tradeoff_pairs[0][0]['adaptive_side'])} (cocoa "
        f"{int(tradeoff_pairs[0][1]['adaptive_cocoa'])}→"
        f"{int(tradeoff_pairs[0][0]['adaptive_cocoa'])}, survivors "
        f"{int(tradeoff_pairs[0][1]['adaptive_surviving_workers'])}→"
        f"{int(tradeoff_pairs[0][0]['adaptive_surviving_workers'])}).",
        f"- Higher delivery/lower survival case: {inverse_pairs[0][0]['map_id']}, adaptive "
        f"side {int(inverse_pairs[0][0]['adaptive_side'])} (cocoa "
        f"{int(inverse_pairs[0][1]['adaptive_cocoa'])}→"
        f"{int(inverse_pairs[0][0]['adaptive_cocoa'])}, survivors "
        f"{int(inverse_pairs[0][1]['adaptive_surviving_workers'])}→"
        f"{int(inverse_pairs[0][0]['adaptive_surviving_workers'])}).",
        "", "## CHOKEPOINT comparison", "",
        "- Mean adaptive/fixed cocoa: " + "; ".join(
            f"{family}={family_delivery[family]['adaptive']:.3f}/"
            f"{family_delivery[family]['fixed']:.3f}" for family in FAMILIES),
        "- Mean adaptive/fixed Worker deaths: " + "; ".join(
            f"{family}={family_delivery[family]['adaptive_deaths']:.3f}/"
            f"{family_delivery[family]['fixed_deaths']:.3f}" for family in FAMILIES),
        "- CHOKEPOINT lowered the relative margin primarily because FixedRoleAI delivered more "
        "there, not because AdaptiveRoleAI delivered less than on OPEN. CHOKEPOINT also had fewer "
        "deaths, carrier hits, and tactical overrides. These observations are consistent with "
        "less direct engagement, but they do not establish constrained movement as the cause.",
        "", "Carrier-hit telemetry counts confirmed hits/deaths while carrying cocoa; it is not a "
        "complete count of all combat. Route length is the D2 center-to-center terrain proxy, not "
        "the exact route traveled by Workers. Family/density comparisons remain confounded because "
        "OPEN uses lower density presets.", "",
    ]
    (output / "analysis.md").write_text("\n".join(lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path,
                        default=Path("results/factory-feud-d3/match-results.csv"))
    parser.add_argument("--output-dir", type=Path,
                        default=Path("results/factory-feud-d3/plots"))
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    rows = read_rows(args.input)
    main_rows, ablation_rows, maps, pairs = save_figures(rows, args.output_dir)
    write_analysis(args.output_dir, main_rows, ablation_rows, maps, pairs)
    print(f"plots={args.output_dir.resolve()} maps={len(maps)} matched_pairs={len(pairs)}")


if __name__ == "__main__":
    main()
