import subprocess
import csv
import javalang
import BPE
import os
import pandas as pd
import numpy as np
import math
import re

def get_line_data(csv_file_path, new_csv_file_path, bpe_ref_path):
    with open(new_csv_file_path, 'a+', newline='', errors='ignore') as w:
        writer = csv.writer(w)
        row = ['commit_hash', 'content', 'old_line_num', 'new_line_num', 'file_content', 'line_label', 'commit_label']
        writer.writerow(row)


    with open(csv_file_path, 'r') as f:
        reader = csv.reader(f)
        for i, row in enumerate(reader):
            if i == 0:
                continue
            commit_id = row[0]

            line_content = row[1]
            line_file = row[3]
            line_num = row[4]
            label = row[7]
            commit_label = row[8]

            cmd = 'git show ' + commit_id + ':' + line_file
            out = subprocess.check_output(cmd, shell=True).decode('utf-8', errors='ignore')
            file_content = out.split('\n')
            clean_content, line_nums = remove_comments(file_content)

            tk_content = bpe_line(clean_content, bpe_ref_path)
            map_line = line_nums[int(line_num)]



            new_row = list()
            new_row.append(commit_id)
            new_row.append(line_content)
            new_row.append(line_num)
            new_row.append(map_line)
            new_row.append(tk_content)
            new_row.append(label)
            new_row.append(commit_label)

            with open(new_csv_file_path, 'a+', newline='', errors='ignore') as w:
                writer = csv.writer(w)
                writer.writerow(new_row)



# 去除其中的注释，并建立新旧行对应关系
def remove_comments(source_code_list):
    in_block = False
    clean_content = []
    line_nums = [-1 for x in range(0,len(source_code_list))]
    j = 0
    l = 0
    for line in source_code_list:
        i = 0
        if not in_block:
            newline = []
        while i < len(line):
            if line[i:i + 2] == '/*' and not in_block:
                in_block = True
                i += 1
            elif line[i:i + 2] == '*/' and in_block:
                in_block = False
                i += 1
            elif not in_block and line[i:i + 2] == '//':
                break
            elif not in_block:
                newline.append(line[i])
            i += 1
        # line_nums[j] = -1
        if newline and not in_block and len(newline) > 0:
            clean_content.append("".join(newline))
            line_nums[j] = l
            l += 1
        j += 1

    return clean_content, line_nums


def bpe_line(content, train_ref):
    tk_list = list(javalang.tokenizer.tokenize(content, ignore_errors=True))
    tk_line = ''
    for i in range(0, len(tk_list) - 1):
        tk_line = tk_line + ' ' + tk_list[i].value
    return (BPE.apply_csv(train_ref, tk_line))



def get_clean_line_data(csv_file_path, store_java_path, bpe_ref_path):

    full_content = list()
    with open(csv_file_path, 'r') as f:
        reader = csv.reader(f)
        for i, row in enumerate(reader):
            if i == 0:
                continue
            line_content = row[1].strip() + '\n'
            # bpe_content = bpe_line(line_content, bpe_ref_path) + '<EOS>'+'\n'
            full_content.append(line_content)

    # full_data = pd.read_csv(csv_file_path)
    # full_data = np.array(full_data)
    # content = full_data[:,1].tolist()
    print(len(full_content))
    f = open(store_java_path, 'w')
    f.writelines(full_content)
    f.close()

def get_commit_list(csv_file_path):
    dataset = pd.read_csv(csv_file_path)
    dataset = np.array(dataset)
    [rows, cols] = dataset.shape

    # 按时间顺序从前到后排序
    # dataset = np.flip(dataset, 0)

    commit_list = list()
    commit_range = list()
    temp_commit_hash = dataset[0,0]
    commit_list.append(temp_commit_hash)
    commit_range.append(0)
    for i in range(0, rows-1):
        commit_hash = dataset[i,0]
        if commit_hash != temp_commit_hash:
            commit_list.append(commit_hash)
            temp_commit_hash = commit_hash
            commit_range.append(i)
    return commit_list, commit_range


def build_train_test_set(all_set_path, csv_file_path, train_ratio, train_store_path, test_store_path):
    commit_list, commit_range = get_commit_list(csv_file_path)
    row_train_size = math.ceil(len(commit_list) * train_ratio)
    test_size = len(commit_list) - row_train_size
    train_range = commit_range[row_train_size]
    test_range = commit_range[test_size]

    all_set = list()
    with open(all_set_path, 'r', encoding="utf8") as f:
        for line in f:
            all_set.append(line)

    test_set = all_set[0:test_range]
    row_train_set = all_set[test_range:len(all_set)]
    train_set = get_clean_train_set(row_train_set, csv_file_path)

    with open(train_store_path, 'w') as tr:
        for line in train_set:
            tr.write(line)

    with open(test_store_path, 'w') as te:
        for line in test_set:
            te.write(line)
    return train_set, test_set

def build_new_train_test_set(all_set_path, csv_file_path, train_ratio,
                             train_store_path, test_store_path, project_file):
    commit_list, commit_range = get_commit_list(csv_file_path)
    row_train_size = math.ceil(len(commit_list) * train_ratio)
    test_size = len(commit_list) - row_train_size
    test_range = commit_range[test_size]

    all_set = list()
    with open(all_set_path, 'r', encoding="utf8") as f:
        for line in f:
            all_set.append(line)

    test_set = all_set[0:test_range]
    train_set = get_project_train_set(project_file)

    with open(train_store_path, 'w') as tr:
        for line in train_set:
            tr.write(line)

    with open(test_store_path, 'w') as te:
        for line in test_set:
            te.write(line)
    return train_set, test_set


def build_context_train_test_set(all_set_path, csv_file_path, train_ratio,
                             train_store_path, test_store_path):
    commit_list, commit_range = get_commit_list(csv_file_path)
    row_train_size = math.ceil(len(commit_list) * train_ratio)
    test_size = len(commit_list) - row_train_size
    test_range = commit_range[test_size]
    all_set = list()
    with open(all_set_path, 'r', encoding="utf8") as f:
        for line in f:
            all_set.append(line)
    test_set = all_set[0:test_range*5]

    row_train_set = all_set[test_range*5:len(all_set)]
    train_set = get_context_clean_train_set(row_train_set, csv_file_path)

    with open(train_store_path, 'w') as tr:
        for line in train_set:
            tr.write(line)

    with open(test_store_path, 'w') as te:
        for line in test_set:
            te.write(line)
    return train_set, test_set


def get_clean_train_set(row_set, csv_file_path):
    dataset = pd.read_csv(csv_file_path)
    dataset = np.array(dataset)

    row_csv = dataset[0:len(row_set)]

    clean_set = list()
    for i in range(0, len(row_csv)):
        if row_csv[i,7] is False:
            clean_set.append(row_set[i])

    return clean_set

def get_project_train_set(project_file):
    train_set = list()
    with open(project_file, 'r', encoding="utf8") as f:
        for line in f:
            if(len(line) > 5):
                train_set.append(line)
    return train_set

def get_context_clean_train_set(row_set, csv_file_path):
    dataset = pd.read_csv(csv_file_path)
    dataset = np.array(dataset)

    row_csv = dataset[0:int(len(row_set)/5)]
    clean_set = list()
    for i in range(0, len(row_csv)):
        if row_csv[i,7] is False:
            clean_set.append(row_set[i])
            clean_set.append(row_set[i+1])
            clean_set.append(row_set[i+2])
            clean_set.append(row_set[i+3])
            clean_set.append(row_set[i+4])
    return clean_set


def identify_code(line):
    pattern = '^[/*]'
    if re.match(pattern, line.strip()) or ''==line.strip() or '{'==line.strip() or '}'==line.strip():
        return True
    else:
        return False


def new_pre_context(line_num, content):
    pre1_num = int(line_num) - 2
    pre1_code_line = content[pre1_num]
    pre2_code_line = '<NONE>'

    while identify_code(pre1_code_line):
        pre1_num = pre1_num - 1
        if pre1_num < 0:
            pre1_code_line = '<NONE>'
            break
        else:
            pre1_code_line = content[pre1_num]

    pre2_num = pre1_num - 1
    if pre2_num > 0:
        pre2_code_line = content[pre2_num]
        while identify_code(pre2_code_line):
            pre2_num = pre2_num - 1
            if pre2_num < 0:
                pre2_code_line = '<NONE>'
                break
            else:
                pre2_code_line = content[pre2_num]
    return pre1_code_line, pre2_code_line

def new_suf_context(line_num, content):
    suf1_num = int(line_num)
    suf1_code_line = content[suf1_num]
    suf2_code_line = '<NONE>'

    while identify_code(suf1_code_line):
        suf1_num = suf1_num + 1
        if suf1_num >= len(content):
            suf1_code_line = '<NONE>'
            break
        else:
            suf1_code_line = content[suf1_num]

    suf2_num = suf1_num + 1
    if suf2_num < len(content):
        suf2_code_line = content[suf2_num]
        while identify_code(suf2_code_line):
            suf2_num = suf2_num + 1
            if suf2_num >= len(content):
                suf2_code_line = '<NONE>'
                break
            else:
                suf2_code_line = content[suf2_num]
    return suf1_code_line, suf2_code_line


def get_line_context(csv_file_path, store_context_path):
    full_content = list()
    with open(csv_file_path, 'r') as f:
        reader = csv.reader(f)
        for i, row in enumerate(reader):
            if i == 0:
                continue
            commit_id = row[0]
            line_content = row[1]
            line_file = row[3]
            line_num = row[4]

            cmd = 'git show ' + commit_id + ':' + line_file
            out = subprocess.check_output(cmd, shell=True).decode('utf-8', errors='ignore')
            file_content = out.split('\n')

            print(commit_id)
            if int(line_num) >= len(file_content)-1:
                pre1_line = '<NONE>'
                pre2_line = '<NONE>'
                suf1_line = '<NONE>'
                suf2_line = '<NONE>'
            else:
                pre1_line, pre2_line = new_pre_context(line_num, file_content)
                suf1_line, suf2_line = new_suf_context(line_num, file_content)


            pre2_line = pre2_line.strip() + '\n'
            pre1_line = pre1_line.strip() + '\n'
            line_content = line_content.strip() + '\n'
            suf1_line = suf1_line.strip() + '\n'
            suf2_line = suf2_line.strip() + '\n'

            full_content.append(pre2_line)
            full_content.append(pre1_line)
            full_content.append(line_content)
            full_content.append(suf1_line)
            full_content.append(suf2_line)

    print(len(full_content))
    f = open(store_context_path, 'w')
    f.writelines(full_content)
    f.close()


def build_exbpe_test_set(exbpe_file_path, csv_file_path, train_ratio, test_store_path):
    commit_list, commit_range = get_commit_list(csv_file_path)
    row_train_size = math.ceil(len(commit_list) * train_ratio)
    test_size = len(commit_list) - row_train_size
    train_range = commit_range[row_train_size]
    test_range = commit_range[test_size]

    all_set = list()
    with open(exbpe_file_path, 'r', encoding="utf8") as f:
        for line in f:
            all_set.append(line)

    test_set = all_set[0:test_range]
    row_train_set = all_set[test_range:len(all_set)]
    train_set = get_clean_train_set(row_train_set, csv_file_path)

    with open(test_store_path, 'w') as te:
        for line in test_set:
            te.write(line)
    return train_set, test_set


if __name__ == '__main__':
    # projects = ['closure-compiler', 'deeplearning4j', 'druid', 'flink', 'jenkins', 'storm', 'robolectric', 'graylog2-server', 'jetty.project',
    #             'jitsi', 'libgdx']
    projects = ["activemq", "druid", "robolectric", "jenkins", "storm", "closure-compiler", "deeplearning4j",
                "graylog2-server", "h2o", "jitsi", "libgdx", "flink", "jetty.project", "jmeter"]
    # projects = ['jmeter']
    for project in projects:
        # os.chdir('/home/qiufangcheng/workspace/OVCNLM/data/project/'+project)

        file_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_add.csv'
        bpe_ref_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/'+project+'/'+ project + '_ref'
        store_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_all.java'
        bpe_file_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_all_bpe.java'
        bpe_context_file_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_context_bpe.java'
        project_train_store_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_project_train.java'
        project_test_store_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_project_test.java'
        context_train_store_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_context_train.java'
        context_test_store_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_context_test.java'

        project_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_train_out.java'

        store_context_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_context.java'



        train_ratio = 0.6

        # 构建不使用bpe分词的训练集与测试集
        exbpe_file_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_all.java'
        csv_file_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_add.csv'
        test_store_path = '/home/qiufangcheng/workspace/OVCNLM/data/dataset/' + project + '/' + project + '_exbpe_test.java'

        build_exbpe_test_set(exbpe_file_path, csv_file_path, train_ratio, test_store_path)
