import math
import csv
import numpy as np



# def evaluate_result(result_csv, eva_csv_true_bug):
# eva_csv_true_bug 结果存储位置
def evaluate_result(result_np, eva_csv_true_bug):

    # eva_headers = ["commit_hash", "total_lines", "bug_lines", "time", "cost"]

    eva_headers = ['commit_hash', 'content', 'file_pre', 'file_new', 'line_num', 'author', 'time', 'bug_introducing', 'commit_label',
     'entropy']

    eva_results = list()
    # result_entropy = get_content(result_csv)
    # np转list
    result_entropy = result_np.tolist()

    evaluate_by_entropy = evaluate_F1Hit_commits(result_entropy, eva_csv_true_bug)
    temp_one_commit = list()
    commit_hash = ""
    lines = 0
    bug_lines = 0
    temp_commit = list()


    for i in range(0, len(result_entropy)):
        this_line = result_entropy[i]
        temp_commit_hash = this_line[0]
        # 时间去掉
        # time = this_line[6]

        if commit_hash == "":
            commit_hash = temp_commit_hash

        # if temp_commit_hash.equals(commit_hash) and i < len(result_entropy) - 1:
        if temp_commit_hash == commit_hash and i < len(result_entropy) - 1:
            temp_one_commit.append(this_line)
            lines += 1
            # if this_line[2].lower().equals("true"):
            if this_line[2] is True:
                bug_lines += 1
        else:
            # if i == result_entropy.size() - 1:
            if i == len(result_entropy) - 1:
                temp_one_commit.append(this_line)
                lines += 1
                # if this_line[2].lower().equals("true"):
                if this_line[2] is True:
                    bug_lines += 1

            cost_results = compute_cost(temp_one_commit, bug_lines)

            temp_commit.append(commit_hash)
            temp_commit.append(str(lines))
            temp_commit.append(str(bug_lines))
            temp_commit.append(str(cost_results.get('recall')))

            # temp_commit[0] = commit_hash
            # temp_commit[1] = str(lines)
            # temp_commit[2] = str(bug_lines)
            # # temp_commit[3] = time
            # temp_commit[3] = str(cost_results.get('recall'))

            eva_results.append(temp_commit)
            commit_hash = ""
            lines = 0
            bug_lines = 0
            temp_one_commit = list()
            temp_commit = list()
            if i < len(result_entropy) - 1:
                i -= 1

    # myCSV.writeToCsv(new File(evaCSVEachCommit), evaHeadersForCommits, evaResult);

    return evaluate_by_entropy


def evaluate_F1Hit_commits(result_entropy, trueBugTestResultPath):
    eva_headers = ["commit_hash", "total_lines", "bug_lines", "mr2Cost", "mapCost", "top1", "top2", "top5", "top10", "aucec5",
     "aucec20", "aucec50"]
    eva_result_for_TP = list()
    eva_result_for_TN = list()
    eva_result_for_TrueBugs = list()
    performance = dict()

    temp_one_commit = list()

    commit_hash = ""
    lines = 0
    bug_lines = 0
    temp_commit = list()

    hit_bug = 0.
    hit_clean = 0.
    all_bug = 0.
    all_clean = 0.
    all_commits = 0.

    for i in range(0, len(result_entropy)):
        this_line = result_entropy[i]

        temp_commit_hash = this_line[0]
        if commit_hash == "":
            commit_hash = temp_commit_hash

        if temp_commit_hash == commit_hash and i < len(result_entropy) - 1:
        # if temp_commit_hash.equals(commit_hash) and i < len(result_entropy) - 1:
            temp_one_commit.append(this_line)
            lines += 1
            if this_line[2] is True:
            # if this_line[2].lower().equals("true"):
                bug_lines += 1
        else:
            # if i == result_entropy.size() - 1:
            if i == len(result_entropy) - 1:
                temp_one_commit.append(this_line)
                lines += 1
                if this_line[2] is True:
                # if this_line[2].lower().equals("true"):
                    bug_lines += 1

            all_commits += 1
            # commit_label = temp_one_commit[0][8].lower()
            # commit_label = temp_one_commit[0][2]
            cost_results = compute_cost(temp_one_commit, bug_lines)

            temp_commit.append(commit_hash)
            temp_commit.append(str(lines))
            temp_commit.append(str(bug_lines))
            temp_commit.append(str(cost_results.get('mr2')))
            temp_commit.append(str(cost_results.get('map')))
            temp_commit.append(str(cost_results.get('top1')))
            temp_commit.append(str(cost_results.get('top2')))
            temp_commit.append(str(cost_results.get('top5')))
            temp_commit.append(str(cost_results.get('top10')))
            temp_commit.append(str(cost_results.get('aucec5')))
            temp_commit.append(str(cost_results.get('aucec20')))
            temp_commit.append(str(cost_results.get('aucec50')))

            # temp_commit[0] = commit_hash
            # temp_commit[1] = str(lines)
            # temp_commit[2] = str(bug_lines)
            # temp_commit[3] = str(cost_results.get('mr2'))
            # temp_commit[4] = str(cost_results.get('map'))
            # temp_commit[5] = str(cost_results.get('top1'))
            # temp_commit[6] = str(cost_results.get('top2'))
            # temp_commit[7] = str(cost_results.get('top5'))
            # temp_commit[8] = str(cost_results.get('top10'))
            # temp_commit[9] = str(cost_results.get('aucec5'))
            # temp_commit[10] = str(cost_results.get('aucec20'))
            # temp_commit[11] = str(cost_results.get('aucec50'))

            # if commit_label == 'true':
            # if commit_label is True:
            hit_bug += 1
            if bug_lines > 0:
                all_bug += 1
                eva_result_for_TP.append(temp_commit)
                eva_result_for_TrueBugs.append(temp_commit)
            else:
                all_clean += 1
            # else:
            #     all_clean += 1
            temp_one_commit = list()
            temp_commit = list()
            commit_hash = ""
            lines = 0
            bug_lines = 0
            if i < len(result_entropy) - 1:
                i -= 1
    print(trueBugTestResultPath + ' Number of changes:' + str(len(eva_result_for_TrueBugs)))
    # myCSV.writeToCsv(new File(trueBugTestResultPath), evaHeaders, evaResultForTrueBugs);
    with open(trueBugTestResultPath, 'w') as f:
        writer = csv.writer(f)
        for line in eva_result_for_TrueBugs:
            writer.writerow(line)

    all_Mr2_TrueBug = np.array(eva_result_for_TrueBugs)[:,3].astype(np.float64)
    all_Map_TrueBug = np.array(eva_result_for_TrueBugs)[:,4].astype(np.float64)
    all_Top1_TrueBug = np.array(eva_result_for_TrueBugs)[:,5].astype(np.float64)
    all_Top2_TrueBug = np.array(eva_result_for_TrueBugs)[:,6].astype(np.float64)
    all_Top5_TrueBug = np.array(eva_result_for_TrueBugs)[:,7].astype(np.float64)
    all_Top10_TrueBug = np.array(eva_result_for_TrueBugs)[:,8].astype(np.float64)
    # all_Aucec5_TrueBug = np.array(eva_result_for_TrueBugs)[:,9].astype(np.float64)
    # all_Aucec20_TrueBug = np.array(eva_result_for_TrueBugs)[:,10].astype(np.float64)
    # all_Aucec50_TrueBug = np.array(eva_result_for_TrueBugs)[:11].astype(np.float64)

    ave_Mr2_TrueBug = np.mean(all_Mr2_TrueBug)
    ave_Map_TrueBug = np.mean(all_Map_TrueBug)
    all_Top1_TrueBug = np.mean(all_Top1_TrueBug)
    all_Top2_TrueBug = np.mean(all_Top2_TrueBug)
    all_Top5_TrueBug = np.mean(all_Top5_TrueBug)
    all_Top10_TrueBug = np.mean(all_Top10_TrueBug)
    # all_Aucec5_TrueBug = np.mean(all_Aucec5_TrueBug)
    # all_Aucec20_TrueBug = np.mean(all_Aucec20_TrueBug)
    # all_Aucec50_TrueBug = np.mean(all_Aucec50_TrueBug)

    performance['allCommits'] = all_commits
    performance['allBugCommits'] = all_bug
    performance['aveMr2TrueBugs'] = ave_Mr2_TrueBug
    performance['aveMapTrueBugs'] = ave_Map_TrueBug
    performance['aveTop1Accuracy'] = all_Top1_TrueBug
    performance['aveTop2Accuracy'] = all_Top2_TrueBug
    performance['aveTop5Accuracy'] = all_Top5_TrueBug
    performance['aveTop10Accuracy'] = all_Top10_TrueBug
    # performance['aveAucec5Accuracy'] = all_Aucec5_TrueBug
    # performance['aveAucec20Accuracy'] = all_Aucec20_TrueBug
    # performance['aveAucec50Accuracy'] = all_Aucec50_TrueBug

    print(ave_Mr2_TrueBug)
    print(ave_Map_TrueBug)
    print(all_Top1_TrueBug)
    print(all_Top5_TrueBug)
    print(all_Top10_TrueBug)
    # print(all_Aucec5_TrueBug)
    # print(all_Aucec20_TrueBug)
    # print(all_Aucec50_TrueBug)


def compute_cost(temp_one_commit, bug_lines):

    cost_results = dict()
    # sorted_commit = temp_one_commit.sort(key=lambda x:x[9], reverse=True)
    sorted_commit = sorted(temp_one_commit, key=lambda x: float(x[3]), reverse=True)

    pre = 0
    recall = 0
    top1 = 0
    top2 = 0
    top5 = 0
    top10 = 0
    mr2 = 0
    map = 0
    aucec5 = 0
    aucec20 = 0
    aucec50 = 0
    cost_area = dict()

    bug_rank = list()
    hit_inspected = 0.
    hit = 0
    lines_cost = dict()

    if bug_lines > 0:
        for i in range(0, len(sorted_commit)):
            this_line = sorted_commit[i]
            # 要修改
            entropy = this_line[3]
            label = this_line[2]
            temp_cost = list()
            if label is True:
            # if label.lower() == 'true':
            #     bug_rank[hit] = i + 1
            #     hit += 1
                bug_rank.append(i+1)
                if 0 == i:
                    top1 = 1
                    top2 = 1
                    top5 = 1
                    top10 = 1
                if 1 == i:
                    top2 = 1
                    top5 = 1
                    top10 = 1
                if i > 1 and i < 5:
                    top5 = 1
                    top10 = 1
                if i >=5 and i < 10:
                    top10 = 1

                hit_inspected = hit_inspected + 1
            lines_cost[i+1] = hit_inspected / bug_lines


    if bug_lines > 0:
        cost_area = generate_aucec(lines_cost)
        aucec5 = cost_area.get(5)
        aucec20 = cost_area.get(20)
        aucec50 = cost_area.get(50)

    if len(bug_rank) > 0:
        mr2 = 1.0 / bug_rank[0]
        map = get_map(bug_rank, bug_lines)

    cost_results['pre'] = pre
    cost_results['recall'] = recall
    cost_results['mr2'] = mr2
    cost_results['map'] = map
    cost_results['top1'] = top1
    cost_results['top2'] = top2
    cost_results['top5'] = top5
    cost_results['top10'] = top10
    cost_results['aucec5'] = aucec5
    cost_results['aucec5'] = aucec20
    cost_results['aucec50'] = aucec50

    return cost_results



def generate_aucec(lines_cost):
    cost_area_added = dict()
    ratio_cost = dict()
    added_area = 0.
    lines = len(lines_cost)
    for i in range(1, 101):
        this_ratio = i / 100.0
        this_ratio_lines = int(this_ratio * lines)
        this_ratio_cost = 0.
        if this_ratio_cost > 0:
            this_ratio_cost = lines_cost.get(this_ratio_lines)
        ratio_cost[i] = this_ratio_cost
        this_ratio_area = 0.
        if 1 == i:
            this_ratio_area = this_ratio_cost * 0.5 * 0.01
        else:
            previous_ratio_cost = ratio_cost.get(i-1)
            this_ratio_area = (previous_ratio_cost + this_ratio_cost) * 0.5 * 0.01
        added_area = added_area + this_ratio_area
        cost_area_added[i] = added_area

    return cost_area_added

def get_map(bug_rank, bug_lines):
    map = 0
    for i in range(0, len(bug_rank)):
        this_rank = bug_rank[i]
        map = map + math.log((i + 1.0) / this_rank)
    return math.exp(map / len(bug_rank))


